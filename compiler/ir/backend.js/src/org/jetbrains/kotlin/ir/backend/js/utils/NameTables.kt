/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.common.isES5IdentifierPart
import org.jetbrains.kotlin.js.common.isES5IdentifierStart
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*
import kotlin.math.abs

abstract class NameScope {
    abstract fun isReserved(name: String): Boolean

    object EmptyScope : NameScope() {
        override fun isReserved(name: String): Boolean = false
    }
}

class NameTable<T>(
    val parent: NameScope = EmptyScope,
    val reserved: MutableSet<String> = mutableSetOf(),
) : NameScope() {
    val names = mutableMapOf<T, String>()

    private val suggestedNameLastIdx = mutableMapOf<String, Int>()

    override fun isReserved(name: String): Boolean {
        return parent.isReserved(name) || name in reserved
    }

    fun declareStableName(declaration: T, name: String) {
        names[declaration] = name
        reserved.add(name)
    }

    fun declareFreshName(declaration: T, suggestedName: String): String {
        val freshName = findFreshName(sanitizeName(suggestedName))
        declareStableName(declaration, freshName)
        return freshName
    }

    private fun findFreshName(suggestedName: String): String {
        if (!isReserved(suggestedName))
            return suggestedName

        var i = suggestedNameLastIdx[suggestedName] ?: 0

        fun freshName() =
            suggestedName + "_" + i

        while (isReserved(freshName())) {
            i++
        }

        suggestedNameLastIdx[suggestedName] = i

        return freshName()
    }
}

fun NameTable<IrDeclaration>.dump(): String =
    "Names: \n" + names.toList().joinToString("\n") { (declaration, name) ->
        val decl: FqName? = (declaration as IrDeclarationWithName).fqNameWhenAvailable
        val declRef = decl ?: declaration
        "---  $declRef => $name"
    }


private const val RESERVED_MEMBER_NAME_SUFFIX = "_k$"

fun Int.toJsIdentifier(): String {
    val first = ('a'.code + (this % 26)).toChar().toString()
    val other = this / 26
    return if (other == 0) {
        first
    } else {
        first + other.toString(Character.MAX_RADIX)
    }
}

private fun List<IrType>.joinTypes(context: JsIrBackendContext): String {
    if (isEmpty()) {
        return ""
    }
    return joinToString("$", "$") { superType -> superType.asString(context) }
}

private fun IrFunction.findOriginallyContainingModule(): IrModuleFragment? {
    if (JsLoweredDeclarationOrigin.isBridgeDeclarationOrigin(origin)) {
        val thisSimpleFunction = this as? IrSimpleFunction
            ?: irError("Bridge must be IrSimpleFunction") {
                withIrEntry("this", this@findOriginallyContainingModule)
            }
        val bridgeFrom = thisSimpleFunction.overriddenSymbols.firstOrNull()
            ?: irError("Couldn't find the overridden function for the bridge") {
                withIrEntry("thisSimpleFunction", thisSimpleFunction)
            }
        return bridgeFrom.owner.findOriginallyContainingModule()
    }
    return (getPackageFragment() as? IrFile)?.module
}

fun calculateJsFunctionSignature(declaration: IrFunction, context: JsIrBackendContext): String {
    val declarationName = declaration.nameIfPropertyAccessor() ?: declaration.getJsNameOrKotlinName().asString()

    val nameBuilder = StringBuilder()
    nameBuilder.append(declarationName)

    if (declaration.visibility === INTERNAL && declaration.parentClassOrNull != null) {
        val containingModule = declaration.findOriginallyContainingModule()
        if (containingModule != null) {
            nameBuilder.append("_\$m_").append(containingModule.name.toString())
        }
    }

    // TODO should we skip type parameters and use upper bound of type parameter when print type of value parameters?
    declaration.typeParameters.ifNotEmpty {
        nameBuilder.append("_\$t")
        forEach { typeParam ->
            nameBuilder.append("_").append(typeParam.name.asString()).append(typeParam.superTypes.joinTypes(context))
        }
    }

    for (parameter in declaration.parameters) {
        when (parameter.kind) {
            IrParameterKind.DispatchReceiver -> continue
            IrParameterKind.ExtensionReceiver -> {
                nameBuilder.append("_r$")
            }
            IrParameterKind.Context -> {
                nameBuilder.append("_c$")
            }
            IrParameterKind.Regular -> {
                nameBuilder.append("_")
            }
        }
        nameBuilder.append(parameter.type.asString(context))
        nameBuilder.append(parameter.type.superTypes().joinTypes(context))
        if (parameter.origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER) {
            nameBuilder.append("?")
        }
    }
    declaration.returnType.let {
        // Return type is only used in signature for inline class and Unit types because
        // they are binary incompatible with supertypes.
        if (context.inlineClassesUtils.isTypeInlined(it) || it.isUnit()) {
            nameBuilder.append("_ret$${it.asString(context)}")
        }
    }

    val signature = abs(nameBuilder.toString().hashCode()).toString(Character.MAX_RADIX)

    // TODO: Use better hashCode
    val sanitizedName = sanitizeName(declarationName, withHash = false)
    return context.globalIrInterner.string("${sanitizedName}_$signature$RESERVED_MEMBER_NAME_SUFFIX")
}

fun jsFunctionSignature(declaration: IrFunction, context: JsIrBackendContext): String {
    require(!declaration.isStaticMethodOfClass)
    require(declaration.dispatchReceiverParameter != null)

    if (declaration.hasStableJsName(context)) {
        val declarationName = declaration.getJsNameOrKotlinName().asString()
        // TODO: Handle reserved suffix in FE
        require(!declarationName.endsWith(RESERVED_MEMBER_NAME_SUFFIX)) {
            "Function ${declaration.fqNameWhenAvailable} uses reserved name suffix \"$RESERVED_MEMBER_NAME_SUFFIX\""
        }
        return declarationName
    }

    val declarationSignature = (declaration as? IrSimpleFunction)?.resolveFakeOverride() ?: declaration
    return calculateJsFunctionSignature(declarationSignature, context)
}

class LocalNameGenerator(val variableNames: NameTable<IrDeclaration>) : IrVisitorVoid() {
    val localLoopNames = NameTable<IrLoop>()
    val localReturnableBlockNames = NameTable<IrReturnableBlock>()

    private val jumpableDeque: Deque<IrExpression> = LinkedList()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        super.visitDeclaration(declaration)
        if (declaration is IrDeclarationWithName) {
            variableNames.declareFreshName(declaration, declaration.name.asString())
        }
    }

    override fun visitBreak(jump: IrBreak) {
        val loop = jump.loop
        if (loop.label == null && loop != jumpableDeque.firstOrNull()) {
            persistLoopName(SYNTHETIC_LOOP_LABEL, loop)
        }

        super.visitBreak(jump)
    }

    override fun visitContinue(jump: IrContinue) {
        val loop = jump.loop
        if (loop.label == null && loop != jumpableDeque.firstOrNull()) {
            persistLoopName(SYNTHETIC_LOOP_LABEL, loop)
        }

        super.visitContinue(jump)
    }

    override fun visitReturn(expression: IrReturn) {
        val targetSymbol = expression.returnTargetSymbol
        if (targetSymbol is IrReturnableBlockSymbol && !expression.isTheLastReturnStatementIn(targetSymbol)) {
            persistReturnableBlockName(SYNTHETIC_BLOCK_LABEL, targetSymbol.owner)
        }

        super.visitReturn(expression)
    }

    override fun visitWhen(expression: IrWhen) {
        jumpableDeque.push(expression)

        super.visitWhen(expression)

        jumpableDeque.pop()
    }

    override fun visitLoop(loop: IrLoop) {
        jumpableDeque.push(loop)

        super.visitLoop(loop)

        jumpableDeque.pop()

        val label = loop.label

        if (label != null) {
            persistLoopName(label, loop)
        }
    }

    private fun persistLoopName(label: String, loop: IrLoop) {
        localLoopNames.declareFreshName(loop, label)
    }

    private fun persistReturnableBlockName(label: String, loop: IrReturnableBlock) {
        localReturnableBlockNames.declareFreshName(loop, label)
    }
}

fun sanitizeName(name: String, withHash: Boolean = true): String {
    if (name.isValidES5Identifier()) return name
    if (name.isEmpty()) return "_"

    // 7 = _ + MAX_INT.toString(Character.MAX_RADIX)
    val builder = StringBuilder(name.length + if (withHash) 7 else 0)

    val first = name.first()

    builder.append(first.mangleIfNot(Char::isES5IdentifierStart))

    for (idx in 1..name.lastIndex) {
        val c = name[idx]
        builder.append(c.mangleIfNot(Char::isES5IdentifierPart))
    }

    return if (withHash) {
        "${builder}_${abs(name.hashCode()).toString(Character.MAX_RADIX)}"
    } else {
        builder.toString()
    }
}

fun IrDeclarationWithName.nameIfPropertyAccessor(): String? {
    if (this is IrSimpleFunction) {
        return when {
            this.correspondingPropertySymbol != null -> {
                val property = this.correspondingPropertySymbol!!.owner
                val name = property.getJsNameOrKotlinName().asString()
                val prefix = when (this) {
                    property.getter -> "get_"
                    property.setter -> "set_"
                    else -> irError("") {
                        withIrEntry("this", this@nameIfPropertyAccessor)
                    }
                }
                prefix + name
            }
            this.origin == JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR -> {
                this.getJsNameOrKotlinName().asString()
                    .removePrefix("<")
                    .removeSuffix(">")
                    .replaceFirst("get-", "get_")
                    .replaceFirst("set-", "set_")
            }
            else -> null
        }
    }
    return null
}

private inline fun Char.mangleIfNot(predicate: Char.() -> Boolean) =
    if (predicate()) this else '_'

private const val SYNTHETIC_LOOP_LABEL = "\$l\$loop"
private const val SYNTHETIC_BLOCK_LABEL = "\$l\$block"

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.js.naming.isValidES5Identifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*
import kotlin.math.abs

fun <T> mapToKey(declaration: T): String {
    return with(JsManglerIr) {
        if (declaration is IrDeclaration && isPublic(declaration)) {
            declaration.hashedMangle.toString()
        } else if (declaration is Signature) {
            declaration.toString().hashMangle.toString()
        } else "key_have_not_generated"
    }
}

fun JsManglerIr.isPublic(declaration: IrDeclaration) =
    declaration.isExported() && declaration !is IrScript && declaration !is IrVariable && declaration !is IrValueParameter

abstract class NameScope {
    abstract fun isReserved(name: String): Boolean

    object EmptyScope : NameScope() {
        override fun isReserved(name: String): Boolean = false
    }
}

class NameTable<T>(
    val parent: NameScope = EmptyScope,
    val reserved: MutableSet<String> = mutableSetOf(),
    val mappedNames: MutableMap<String, String> = mutableMapOf()
) : NameScope() {
    val names = mutableMapOf<T, String>()
    val suggestedNameLastIdx = mutableMapOf<String, Int>()

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

sealed class Signature
data class StableNameSignature(val name: String) : Signature()
data class BackingFieldSignature(val field: IrField) : Signature()


fun fieldSignature(field: IrField): Signature {
    if (field.isEffectivelyExternal()) {
        return StableNameSignature(field.name.identifier)
    }

    return BackingFieldSignature(field)
}

fun jsFunctionSignature(declaration: IrSimpleFunction): Signature {
    require(!declaration.isStaticMethodOfClass)
    require(declaration.dispatchReceiverParameter != null)

    val declarationName = declaration.getJsNameOrKotlinName().asString()

    val needsStableName = declaration.origin == JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION ||
            declaration.hasStableJsName() ||
            (declaration as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions

    if (needsStableName) {
        return StableNameSignature(declarationName)
    }

    val nameBuilder = StringBuilder()

    nameBuilder.append(declarationName)

    // TODO should we skip type parameters and use upper bound of type parameter when print type of value parameters?
    declaration.typeParameters.ifNotEmpty {
        nameBuilder.append("_\$t")
        joinTo(nameBuilder, "") { "_${it.name.asString()}" }
    }
    declaration.extensionReceiverParameter?.let {
        nameBuilder.append("_r$${it.type.asString()}")
    }
    declaration.valueParameters.ifNotEmpty {
        joinTo(nameBuilder, "") { "_${it.type.asString()}" }
    }
    declaration.returnType.let {
        // Return type is only used in signature for inline class and Unit types because
        // they are binary incompatible with supertypes.
        if (it.getJsInlinedClass() != null || it.isUnit()) {
            nameBuilder.append("_ret$${it.asString()}")
        }
    }

    val signature = nameBuilder.toString()


    return StableNameSignature(sanitizeName(declarationName) + "_" + abs(signature.hashCode()))
}

class NameTables(
    packages: Iterable<IrPackageFragment>,
    reservedForGlobal: Set<String> = emptySet(),
    reservedForMember: Set<String> = emptySet(),
    val mappedNames: MutableMap<String, String> = mutableMapOf()
) {
    val globalNames: NameTable<IrDeclaration>
    private val memberNames: NameTable<IrField>
    private val loopNames = mutableMapOf<IrLoop, String>()

    init {
        val stableNamesCollector = StableNamesCollector()
        packages.forEach { it.acceptChildrenVoid(stableNamesCollector) }

        globalNames = NameTable(
            reserved = (stableNamesCollector.staticNames + reservedForGlobal).toMutableSet(),
            mappedNames = mappedNames
        )

        memberNames = NameTable(
            reserved = (stableNamesCollector.memberNames + reservedForMember).toMutableSet(),
            mappedNames = mappedNames
        )

        mappedNames.addAllIfAbsent(mappedNames)

        val classDeclaration = mutableListOf<IrClass>()
        for (p in packages) {
            for (declaration in p.declarations) {
                processTopLevelLocalDecl(declaration)
                declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase) {
                        processNonTopLevelLocalDecl(declaration)
                        super.visitDeclaration(declaration)
                    }

                    override fun visitDeclarationReference(expression: IrDeclarationReference) {
                        val decl = expression.symbol.owner as IrDeclaration
                        processNonTopLevelLocalDecl(decl)
                        super.visitDeclarationReference(expression)
                    }
                })
                if (declaration is IrScript) {
                    for (memberDecl in declaration.statements) {
                        if (memberDecl is IrDeclaration) {
                            processTopLevelLocalDecl(memberDecl)
                            if (memberDecl is IrClass) {
                                classDeclaration += memberDecl
                            }
                        }
                    }
                }
            }
        }

        for (declaration in classDeclaration) {
            processNonTopLevelLocalDecl(declaration)
        }

        for (p in packages) {
            for (declaration in p.declarations) {
                processNonTopLevelLocalDecl(declaration)
            }
        }
    }

    private fun acceptExternalClass(declaration: IrClass) {
        for (child in declaration.declarations) {
            if (child is IrClass) {
                acceptExternalClass(child)
            }
        }
    }

    private fun acceptNonExternalClass(declaration: IrClass) {
        for (memberDecl in declaration.declarations) {
            when (memberDecl) {
                is IrField ->
                    generateNameForMemberField(memberDecl)
            }
        }
    }

    private fun processNonTopLevelLocalDecl(declaration: IrDeclaration) {
        if (declaration is IrClass) {
            if (declaration.isEffectivelyExternal()) {
                acceptExternalClass(declaration)
            } else {
                acceptNonExternalClass(declaration)
            }
        }
    }

    private fun processReferencedDecl(declaration: IrDeclaration) {
        if (declaration is IrClass) {
            if (declaration.isEffectivelyExternal()) {
                acceptExternalClass(declaration)
            } else {
                acceptNonExternalClass(declaration)
            }
        }
    }

    private fun <T, K> MutableMap<T, K>.addAllIfAbsent(other: Map<T, K>) {
        this += other.filter { it.key !in this }
    }

    private fun packagesAdded() = mappedNames.isEmpty()

    fun merge(files: List<IrPackageFragment>, additionalPackages: List<IrPackageFragment>) {
        val packages = mutableListOf<IrPackageFragment>().also { it.addAll(files) }
        if (packagesAdded()) packages.addAll(additionalPackages)

        val table = NameTables(packages, globalNames.reserved, memberNames.reserved, mappedNames)

        globalNames.names.addAllIfAbsent(table.globalNames.names)
        memberNames.names.addAllIfAbsent(table.memberNames.names)
        loopNames.addAllIfAbsent(table.loopNames)

        globalNames.reserved.addAll(table.globalNames.reserved)
        memberNames.reserved.addAll(table.memberNames.reserved)
    }

    private fun generateNameForMemberField(field: IrField) {
        require(!field.isTopLevel)
        require(!field.isStatic)
        val signature = fieldSignature(field)

        if (field.isEffectivelyExternal()) {
            memberNames.declareStableName(field, field.name.identifier)
        } else {
            memberNames.declareFreshName(field, "_" + sanitizeName(field.name.asString()))
        }
    }

    @Suppress("unused")
    fun dump(): String {
        return "Global names:\n${globalNames.dump()}"
    }

    fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): String {
        val global: String? = globalNames.names[declaration]
        if (global != null) return global

        if (declaration is IrTypeParameter)
        // Investigate org.jetbrains.kotlin.js.test.ir.semantics.IrJsCodegenBoxTestGenerated.Coroutines#testInlineSuspendFunction_1_3
            return "WTF"

        return mappedNames[mapToKey(declaration)]
            ?: error("Can't find name for declaration ${declaration.render()}")
    }

    fun getNameForMemberField(field: IrField): String {
        val signature = fieldSignature(field)
        val name = memberNames.names[signature] ?: mappedNames[mapToKey(signature)]

        // TODO investigate
        if (name == null) {
            return sanitizeName(field.name.asString()) + "__error"
        }

        return name
    }

    private fun processTopLevelLocalDecl(declaration: IrDeclaration) {
        when {
            declaration !is IrDeclarationWithName ->
                return

            declaration.isEffectivelyExternal() && (declaration.getJsModule() == null || declaration.isJsNonModule()) ->
                globalNames.declareStableName(declaration, declaration.getJsNameOrKotlinName().identifier)

            else ->
                globalNames.declareFreshName(declaration, declaration.name.asString())
        }
    }

}

class LocalNameGenerator(parentScope: NameScope) : IrElementVisitorVoid {
    val variableNames = NameTable<IrDeclarationWithName>(parentScope)
    val localLoopNames = NameTable<IrLoop>()

    private val breakableDeque: Deque<IrExpression> = LinkedList()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        super.visitDeclaration(declaration)
        if (declaration is IrDeclarationWithName) {
            val name = declaration.name.asString()
            if (name.endsWith("_hack")) {
                variableNames.declareStableName(declaration, name)
            } else {
                variableNames.declareFreshName(declaration, name)
            }
        }
    }

    override fun visitBreak(jump: IrBreak) {
        val loop = jump.loop
        if (loop.label == null && loop != breakableDeque.firstOrNull()) {
            persistLoopName(SYNTHETIC_LOOP_LABEL, loop)
        }

        super.visitBreak(jump)
    }

    override fun visitWhen(expression: IrWhen) {
        breakableDeque.push(expression)

        super.visitWhen(expression)

        breakableDeque.pop()
    }

    override fun visitLoop(loop: IrLoop) {
        breakableDeque.push(loop)

        super.visitLoop(loop)

        breakableDeque.pop()

        val label = loop.label

        if (label != null) {
            persistLoopName(label, loop)
        }
    }

    private fun persistLoopName(label: String, loop: IrLoop) {
        localLoopNames.declareFreshName(loop, label)
    }
}


fun sanitizeName(name: String): String {
    if (name.isValidES5Identifier()) return name
    if (name.isEmpty()) return "_"

    val builder = StringBuilder()

    val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
    builder.append(first)

    for (idx in 1..name.lastIndex) {
        val c = name[idx]
        builder.append(if (c.isES5IdentifierPart()) c else '_')
    }

    return builder.toString()
}

private const val SYNTHETIC_LOOP_LABEL = "\$l\$break"

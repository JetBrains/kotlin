/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.file
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
import kotlin.collections.set
import kotlin.math.abs

// TODO remove direct usages of [mapToKey] from [NameTable] & co and move it to scripting & REPL infrastructure. Review usages.
private fun <T> mapToKey(declaration: T): String {
    return with(JsManglerIr) {
        if (declaration is IrDeclaration) {
            declaration.hashedMangle { null }.toString()
        } else if (declaration is String) {
            declaration.hashMangle.toString()
        } else {
            error("Key is not generated for " + declaration?.let { it::class.simpleName })
        }
    }
}

abstract class NameScope {
    abstract fun isReserved(name: String): Boolean

    object EmptyScope : NameScope() {
        override fun isReserved(name: String): Boolean = false
    }
}

class NameTable<T>(
    val parent: NameScope = EmptyScope,
    val reserved: MutableSet<String> = mutableSetOf(),
    val mappedNames: MutableMap<String, String>? = null
) : NameScope() {
    val names = mutableMapOf<T, String>()

    private val suggestedNameLastIdx = mutableMapOf<String, Int>()

    override fun isReserved(name: String): Boolean {
        return parent.isReserved(name) || name in reserved
    }

    fun declareStableName(declaration: T, name: String) {
        names[declaration] = name
        reserved.add(name)
        mappedNames?.set(mapToKey(declaration), name)
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

fun jsFunctionSignature(declaration: IrFunction, context: JsIrBackendContext?): String {
    require(!declaration.isStaticMethodOfClass)
    require(declaration.dispatchReceiverParameter != null)

    val declarationName = declaration.getJsNameOrKotlinName().asString()

    if (declaration.hasStableJsName(context)) {
        // TODO: Handle reserved suffix in FE
        require(!declarationName.endsWith(RESERVED_MEMBER_NAME_SUFFIX)) {
            "Function ${declaration.fqNameWhenAvailable} uses reserved name suffix \"$RESERVED_MEMBER_NAME_SUFFIX\""
        }
        return declarationName
    }

    val nameBuilder = StringBuilder()

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

    // TODO: Use better hashCode
    return sanitizeName(declarationName) + "_" + abs(signature.hashCode()).toString(Character.MAX_RADIX) + RESERVED_MEMBER_NAME_SUFFIX
}

class NameTables(
    packages: Iterable<IrPackageFragment>,
    reservedForGlobal: MutableSet<String> = mutableSetOf(),
    reservedForMember: MutableSet<String> = mutableSetOf(),
    val mappedNames: MutableMap<String, String>? = null,
    private val context: JsIrBackendContext? = null
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

        for (p in packages) {
            for (declaration in p.declarations) {
                processTopLevelLocalDecl(declaration)
                processNonTopLevelLocalDecl(declaration)
                declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase) {
                        processNonTopLevelLocalDecl(declaration)
                        super.visitDeclaration(declaration)
                    }
                })
                if (declaration is IrScript) {
                    for (memberDecl in declaration.statements) {
                        if (memberDecl is IrDeclaration) {
                            processTopLevelLocalDecl(memberDecl)
                            if (memberDecl is IrClass) {
                                processNonTopLevelLocalDecl(memberDecl)
                            }
                        }
                    }
                }
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
            if (!declaration.isEffectivelyExternal()) {
                acceptNonExternalClass(declaration)
            }
        }
    }

    private fun <T, K> MutableMap<T, K>.addAllIfAbsent(other: Map<T, K>) {
        this += other.filter { it.key !in this }
    }

    private fun packagesAdded() = mappedNames.isNullOrEmpty()

    fun merge(files: List<IrPackageFragment>, additionalPackages: List<IrPackageFragment>) {
        val packages = mutableListOf<IrPackageFragment>().also { it.addAll(files) }
        if (packagesAdded()) packages.addAll(additionalPackages)

        val table = NameTables(
            packages,
            globalNames.reserved,
            memberNames.reserved,
            mappedNames,
            context
        )

        globalNames.names.addAllIfAbsent(table.globalNames.names)
        memberNames.names.addAllIfAbsent(table.memberNames.names)
        loopNames.addAllIfAbsent(table.loopNames)

        globalNames.reserved.addAll(table.globalNames.reserved)
        memberNames.reserved.addAll(table.memberNames.reserved)
    }

    private fun generateNameForMemberField(field: IrField) {
        require(!field.isTopLevel)
        require(!field.isStatic)

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

        mappedNames?.get(mapToKey(declaration))?.let {
            return it
        }

        error("Can't find name for declaration ${declaration.render()}")
    }

    fun getNameForMemberField(field: IrField): String {
        val name = memberNames.names[field] ?: mappedNames?.get(mapToKey(field))

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

            // TODO: Handle JsQualifier
            declaration.isEffectivelyExternal() && (declaration.getJsModule() == null || declaration.isJsNonModule()) -> {
                if (declaration.file.getJsModule() != null) {
                    globalNames.declareFreshName(declaration, declaration.name.asString())
                } else {
                    globalNames.declareStableName(declaration, declaration.getJsNameOrKotlinName().identifier)
                }
            }

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
            variableNames.declareFreshName(declaration, declaration.name.asString())
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

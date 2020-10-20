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
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*

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

class NameTable<T>(
    val parent: NameTable<*>? = null,
    val reserved: MutableSet<String> = mutableSetOf(),
    val sanitizer: (String) -> String = ::sanitizeName,
    val mappedNames: MutableMap<String, String> = mutableMapOf()
) {
    var finished = false
    val names = mutableMapOf<T, String>()

    private fun isReserved(name: String): Boolean {
        if (parent != null && parent.isReserved(name))
            return true
        return name in reserved
    }

    fun declareStableName(declaration: T, name: String) {
        if (parent != null) assert(parent.finished)
        assert(!finished)
        names[declaration] = name
        reserved.add(name)
        mappedNames[mapToKey(declaration)] = name
    }

    fun declareFreshName(declaration: T, suggestedName: String): String {
        val freshName = findFreshName(sanitizer(suggestedName))
        declareStableName(declaration, freshName)
        return freshName
    }

    private fun findFreshName(suggestedName: String): String {
        if (!isReserved(suggestedName))
            return suggestedName

        var i = 0

        fun freshName() =
            suggestedName + "_" + i

        while (isReserved(freshName())) {
            i++
        }
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
data class ParameterTypeBasedSignature(val mangledName: String, val suggestedName: String) : Signature()

fun fieldSignature(field: IrField): Signature {
    if (field.isEffectivelyExternal()) {
        return StableNameSignature(field.name.identifier)
    }

    return BackingFieldSignature(field)
}

fun functionSignature(declaration: IrFunction): Signature {
    require(!declaration.isStaticMethodOfClass)
    require(declaration.dispatchReceiverParameter != null)

    val declarationName = declaration.getJsNameOrKotlinName().asString()
    val stableName = StableNameSignature(declarationName)

    if (declaration.origin == JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION) {
        return stableName
    }
    if (declaration.isEffectivelyExternal()) {
        return stableName
    }
    if (declaration.getJsName() != null) {
        return stableName
    }
    // Handle names for special functions
    if (declaration is IrSimpleFunction && declaration.isMethodOfAny()) {
        return stableName
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
        if (it.isInlined() || it.isUnit()) {
            nameBuilder.append("_ret$${it.asString()}")
        }
    }

    val signature = nameBuilder.toString()

    // TODO: Check reserved names
    return ParameterTypeBasedSignature(signature, declarationName)
}

class NameTables(
    packages: List<IrPackageFragment>,
    reservedForGlobal: MutableSet<String> = mutableSetOf(),
    reservedForMember: MutableSet<String> = mutableSetOf(),
    val mappedNames: MutableMap<String, String> = mutableMapOf()
) {
    val globalNames: NameTable<IrDeclaration>
    private val memberNames: NameTable<Signature>
    private val localNames = mutableMapOf<IrDeclaration, NameTable<IrDeclaration>>()
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
                generateNamesForTopLevelDecl(declaration)
                if (declaration is IrScript) {
                    for (memberDecl in declaration.declarations) {
                        generateNamesForTopLevelDecl(memberDecl)
                        if (memberDecl is IrClass) {
                            classDeclaration += memberDecl
                        }
                    }
                }
            }
        }

        globalNames.finished = true

        for (declaration in classDeclaration) {
            acceptDeclaration(declaration)
        }

        for (p in packages) {
            for (declaration in p.declarations) {
                acceptDeclaration(declaration)
            }
        }
    }

    private fun acceptDeclaration(declaration: IrDeclaration) {
        val localNameGenerator = LocalNameGenerator(declaration)

        if (declaration is IrClass) {
            if (declaration.isEffectivelyExternal()) {
                declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                        val parent = declaration.parent
                        if (parent is IrClass && !parent.isEnumClass) {
                            generateNameForMemberFunction(declaration)
                        }
                    }

                    override fun visitField(declaration: IrField) {
                        val parent = declaration.parent
                        if (parent is IrClass && !parent.isEnumClass) {
                            generateNameForMemberField(declaration)
                        }
                    }
                })
            } else {
                declaration.thisReceiver!!.acceptVoid(localNameGenerator)
                for (memberDecl in declaration.declarations) {
                    memberDecl.acceptChildrenVoid(LocalNameGenerator(memberDecl))
                    when (memberDecl) {
                        is IrSimpleFunction ->
                            generateNameForMemberFunction(memberDecl)
                        is IrField ->
                            generateNameForMemberField(memberDecl)
                    }
                }
            }
        } else {
            declaration.acceptChildrenVoid(localNameGenerator)
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
        localNames.addAllIfAbsent(table.localNames)
        loopNames.addAllIfAbsent(table.loopNames)

        globalNames.reserved.addAll(table.globalNames.reserved)
        memberNames.reserved.addAll(table.memberNames.reserved)
    }

    private fun generateNameForMemberField(field: IrField) {
        require(!field.isTopLevel)
        require(!field.isStatic)
        val signature = fieldSignature(field)

        if (field.isEffectivelyExternal()) {
            memberNames.declareStableName(signature, field.name.identifier)
        } else {
            memberNames.declareFreshName(signature, "_" + sanitizeName(field.name.asString()))
        }
    }

    private fun generateNameForMemberFunction(declaration: IrSimpleFunction) {
        when (val signature = functionSignature(declaration)) {
            is StableNameSignature -> memberNames.declareStableName(signature, signature.name)
            is ParameterTypeBasedSignature -> memberNames.declareFreshName(signature, signature.suggestedName)
        }
    }

    @Suppress("unused")
    fun dump(): String {
        val local = localNames.toList().joinToString("\n") { (decl, table) ->
            val declRef = (decl as? IrDeclarationWithName)?.fqNameWhenAvailable ?: decl
            "\nLocal names for $declRef:\n${table.dump()}\n"
        }
        return "Global names:\n${globalNames.dump()}" +
                //   "\nMember names:\n${memberNames.dump()}" +
                "\nLocal names:\n$local\n"
    }

    fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): String {
        val global: String? = globalNames.names[declaration]
        if (global != null) return global

        var parent: IrDeclarationParent = declaration.parent
        while (parent is IrDeclaration) {
            val parentLocalNames = localNames[parent]
            if (parentLocalNames != null) {
                val localName = parentLocalNames.names[declaration]
                if (localName != null)
                    return localName
            }
            parent = parent.parent
        }

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

    fun getNameForMemberFunction(function: IrSimpleFunction): String {
        val signature = functionSignature(function)
        val name = memberNames.names[signature] ?: mappedNames[mapToKey(signature)]

        // TODO Add a compiler flag, which enables this behaviour
        // TODO remove in DCE
        if (name == null) {
            return sanitizeName(function.name.asString()) + "__error" // TODO one case is a virtual method of an abstract class with no implementation
        }

        return name
    }

    private fun generateNamesForTopLevelDecl(declaration: IrDeclaration) {
        when {
            declaration !is IrDeclarationWithName ->
                return

            declaration.isEffectivelyExternal() && (declaration.getJsModule() == null || declaration.isJsNonModule()) ->
                globalNames.declareStableName(declaration, declaration.getJsNameOrKotlinName().identifier)

            else ->
                globalNames.declareFreshName(declaration, declaration.name.asString())
        }
    }

    inner class LocalNameGenerator(parentDeclaration: IrDeclaration) : IrElementVisitorVoid {
        val table = NameTable<IrDeclaration>(globalNames)

        private val breakableDeque: Deque<IrExpression> = LinkedList()

        init {
            localNames[parentDeclaration] = table
        }

        private val localLoopNames = NameTable<IrLoop>()
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitDeclaration(declaration: IrDeclarationBase) {
            if (declaration is IrDeclarationWithName && declaration is IrSymbolOwner) {
                table.declareFreshName(declaration, declaration.name.asString())
            }
            super.visitDeclaration(declaration)
        }

        override fun visitBreak(jump: IrBreak) {
            val loop = jump.loop
            if (loop != breakableDeque.firstOrNull()) {
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
            loopNames[loop] = localLoopNames.names[loop]!!
        }
    }

    fun getNameForLoop(loop: IrLoop): String? =
        loopNames[loop]
}


fun sanitizeName(name: String): String {
    if (name.isEmpty()) return "_"

    val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
    return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
}

private const val SYNTHETIC_LOOP_LABEL = "\$l\$break"

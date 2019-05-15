/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class NameTable<T>(
    val parent: NameTable<T>? = null,
    private val reserved: MutableSet<String> = mutableSetOf()
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
    }

    fun declareFreshName(declaration: T, suggestedName: String) {
        val freshName = findFreshName(sanitizeName(suggestedName))
        declareStableName(declaration, freshName)
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


class NameTables(packages: List<IrPackageFragment>) {
    private val globalNames: NameTable<IrDeclaration>
    private val memberNames: NameTable<IrDeclaration>
    private val localNames = mutableMapOf<IrDeclaration, NameTable<IrDeclaration>>()
    private val loopNames = mutableMapOf<IrLoop, String>()

    init {
        val stableNamesCollector = StableNamesCollector()
        packages.forEach { it.acceptChildrenVoid(stableNamesCollector) }

        globalNames = NameTable(reserved = stableNamesCollector.staticNames)
        memberNames = NameTable(reserved = stableNamesCollector.memberNames)

        for (p in packages) {
            for (declaration in p.declarations) {
                generateNamesForTopLevelDecl(declaration)
            }
        }

        globalNames.finished = true

        for (p in packages) {
            for (declaration in p.declarations) {
                if (declaration.isEffectivelyExternal())
                    continue

                val localNameGenerator = LocalNameGenerator(declaration)

                if (declaration is IrClass) {
                    declaration.thisReceiver!!.acceptVoid(localNameGenerator)
                    for (memberDecl in declaration.declarations) {
                        memberDecl.acceptChildrenVoid(LocalNameGenerator(memberDecl))
                    }
                } else {
                    declaration.acceptChildrenVoid(localNameGenerator)
                }
            }
        }
    }

    @Suppress("unused")
    fun dump(): String {
        val local = localNames.toList().joinToString("\n") { (decl, table) ->
            val declRef = (decl as? IrDeclarationWithName)?.fqNameWhenAvailable ?: decl
            "\nLocal names for $declRef:\n${table.dump()}\n"
        }
        return "Global names:\n${globalNames.dump()}" +
                "\nMember names:\n${memberNames.dump()}" +
                "\nLocal names:\n$local\n"
    }

    fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): String {
        val global: String? = globalNames.names[declaration]
        if (global != null) return global

        if (declaration is IrTypeParameter) {
            // TODO: Fix type parameters
            return declaration.name.identifier
        }

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

        error("Can't find name for declaration ${declaration.fqNameWhenAvailable}")
    }


    private fun generateNamesForTopLevelDecl(declaration: IrDeclaration) {
        when {
            declaration !is IrDeclarationWithName ->
                return

            declaration.isEffectivelyExternal() ->
                globalNames.declareStableName(declaration, declaration.getJsNameOrKotlinName().identifier)

            else ->
                globalNames.declareFreshName(declaration, declaration.name.asString())
        }
    }

    inner class LocalNameGenerator(parentDeclaration: IrDeclaration) : IrElementVisitorVoid {
        val table = NameTable(globalNames)

        init {
            localNames[parentDeclaration] = table
        }

        private val localLoopNames = NameTable<IrLoop>()
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitValueParameter(declaration: IrValueParameter) {
            val parentFunction = declaration.parent as? IrFunction
            if ((declaration.origin == IrDeclarationOrigin.INSTANCE_RECEIVER && declaration.name.isSpecial) ||
                (parentFunction != null && declaration == parentFunction.dispatchReceiverParameter)
            )
                table.declareStableName(declaration, Namer.IMPLICIT_RECEIVER_NAME)
            else
                super.visitValueParameter(declaration)
        }

        override fun visitDeclaration(declaration: IrDeclaration) {
            if (declaration is IrDeclarationWithName && declaration is IrSymbolOwner) {
                table.declareFreshName(declaration, declaration.name.asString())
            }
            super.visitDeclaration(declaration)
        }

        override fun visitLoop(loop: IrLoop) {
            val label = loop.label
            if (label != null) {
                localLoopNames.declareFreshName(loop, label)
                loopNames[loop] = localLoopNames.names[loop]!!
            }
            super.visitLoop(loop)
        }
    }

    fun getNameForLoop(loop: IrLoop): String? =
        if (loop.label == null)
            null
        else
            loopNames[loop]!!
}

// TODO: implement without JsScope
class LegacyMemberNameGenerator(val scope: JsScope) {

    private val fieldCache = mutableMapOf<IrField, JsName>()
    private val functionCache = mutableMapOf<IrFunction, JsName>()

    fun getNameForMemberField(field: IrField): JsName {
        return fieldCache.getOrPut(field) { getNewNameForField(field) }
    }

    fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        return functionCache.getOrPut(function) { getNewNameForFunction(function) }
    }

    private fun getNewNameForField(f: IrField): JsName {
        require(!f.isTopLevel)
        require(!f.isStatic)

        if (f.isEffectivelyExternal()) {
            return scope.declareName(f.name.identifier)
        }

        val parentName = (f.parent as IrDeclarationWithName).name.asString()
        val name = "${f.name.asString()}_$parentName"

        return scope.declareFreshName(sanitizeName(name))
    }

    private fun getNewNameForFunction(declaration: IrSimpleFunction): JsName {
        require(!declaration.isStaticMethodOfClass)
        require(declaration.dispatchReceiverParameter != null)

        val declarationName = declaration.getJsNameOrKotlinName().asString()

        if (declaration.origin == JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION) {
            return scope.declareName(declarationName)
        }

        if (declaration.isEffectivelyExternal()) {
            return scope.declareName(declarationName)
        }
        declaration.getJsName()?.let { jsName ->
            return scope.declareName(jsName)
        }

        val nameBuilder = StringBuilder()

        // Handle names for special functions
        if (declaration.isMethodOfAny()) {
            return scope.declareName(declarationName)
        }

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

        // TODO: Check reserved names

        return scope.declareName(sanitizeName(nameBuilder.toString()))
    }
}


fun sanitizeName(name: String): String {
    if (name.isEmpty()) return "_"

    val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
    return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
}
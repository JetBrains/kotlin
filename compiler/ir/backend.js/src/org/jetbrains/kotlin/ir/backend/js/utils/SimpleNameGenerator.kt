/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

// TODO: this class has to be reimplemented soon
class SimpleNameGenerator : NameGenerator {

    private val nameCache = mutableMapOf<IrDeclaration, JsName>()
    private val loopCache = mutableMapOf<IrLoop, JsName>()

    override fun getNameForSymbol(symbol: IrSymbol, context: JsGenerationContext): JsName =
        getNameForDeclaration(symbol.owner as IrDeclarationWithName, context)

    override fun getNameForLoop(loop: IrLoop, context: JsGenerationContext): JsName? = loop.label?.let {
        loopCache.getOrPut(loop) { context.currentScope.declareFreshName(sanitizeName(loop.label!!)) }
    }

    override fun getNameForType(type: IrType, context: JsGenerationContext) =
        getNameForDeclaration(type.classifierOrFail.owner as IrDeclarationWithName, context)

    private fun getNameForDeclaration(declaration: IrDeclarationWithName, context: JsGenerationContext): JsName {
        return nameCache.getOrPut(declaration) { getNewNameForDeclaration(declaration, context) }
    }

    private fun getNewNameForDeclaration(declaration: IrDeclarationWithName, context: JsGenerationContext): JsName {
        var nameDeclarator: (String) -> JsName = context.currentScope::declareName

        val declarationName = declaration.getJsNameOrKotlinName().asString()

        if (declaration is IrProperty) {
            return context.currentScope.declareName(declaration.getJsNameOrKotlinName().asString())
        }

        if (declaration is IrSimpleFunction && declaration.origin == JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION) {
            return nameDeclarator(declarationName)
        }

        if (declaration.isEffectivelyExternal()) {
            if (declaration is IrConstructor)
                return getNameForDeclaration(declaration.parentAsClass, context)

            if (declaration is IrClass && declaration.parent is IrClass) {
                val parentName = getNameForDeclaration(declaration.parentAsClass, context)
                if (declaration.isCompanion) {
                    // External companions are class references
                    return parentName
                }
                return context.currentScope.declareFreshName(parentName.ident + "$" + declarationName)
            }
            return nameDeclarator(declarationName)
        }

        val jsName = declaration.getJsName()
        if (jsName != null) {
            return context.currentScope.declareName(jsName)
        }

        val nameBuilder = StringBuilder()
        when (declaration) {
            is IrValueParameter -> {
                if ((context.currentFunction is IrConstructor && declaration.origin == IrDeclarationOrigin.INSTANCE_RECEIVER && declaration.name.isSpecial) ||
                    declaration == context.currentFunction?.dispatchReceiverParameter
                )
                    nameBuilder.append(Namer.IMPLICIT_RECEIVER_NAME)
                else if (declaration == context.currentFunction?.extensionReceiverParameter) {
                    nameBuilder.append(Namer.EXTENSION_RECEIVER_NAME)
                } else {
                    val declaredName = declarationName
                    nameBuilder.append(declaredName)
                    if (declaredName.startsWith("\$")) {
                        nameBuilder.append('.')
                        nameBuilder.append(declaration.index)
                    }
                    nameDeclarator = context.currentScope::declareFreshName
                }
            }
            is IrField -> {
                nameBuilder.append(declarationName)
                if (declaration.isTopLevel) {
                    nameDeclarator = context.staticContext.rootScope::declareFreshName
                } else {
                    nameBuilder.append('.')
                    nameBuilder.append(getNameForDeclaration(declaration.parent as IrDeclarationWithName, context))
                    if (declaration.visibility == Visibilities.PRIVATE) nameDeclarator = context.currentScope::declareFreshName
                }
            }
            is IrClass -> {
                if (declaration.isCompanion) {
                    nameBuilder.append(getNameForDeclaration(declaration.parent as IrDeclarationWithName, context))
                    nameBuilder.append('.')
                }

                nameBuilder.append(declarationName)

                (declaration.parent as? IrClass)?.let {
                    nameBuilder.append("$")
                    nameBuilder.append(getNameForDeclaration(it, context))
                }


                nameDeclarator = context.staticContext.rootScope::declareFreshName
                if (declaration.kind == ClassKind.OBJECT || declaration.name.isSpecial || declaration.visibility == Visibilities.LOCAL) {
                    val parent = declaration.parent
                    when (parent) {
                        is IrDeclarationWithName -> nameBuilder.append(getNameForDeclaration(parent, context))
                        is IrPackageFragment -> nameBuilder.append(parent.fqName.asString())
                    }
                }

                // TODO: remove asap `NameGenerator` is implemented
                (declaration.parent as? IrPackageFragment)?.let {
                    if (declaration.isInline && it.fqName.asString() != "kotlin") {
                        nameBuilder.append("_FIX")
                    }
                }

            }
            is IrConstructor -> {
                nameBuilder.append(getNameForDeclaration(declaration.parent as IrClass, context))
            }
            is IrVariable -> {
                nameBuilder.append(declaration.name.identifier)
                nameDeclarator = context.currentScope::declareFreshName
            }
            is IrSimpleFunction -> {

                // Handle names for special functions
                if (declaration.isEqualsInheritedFromAny()) {
                    return context.staticContext.rootScope.declareName("equals")
                }

                if (declaration.isStaticMethodOfClass) {
                    nameBuilder.append(getNameForDeclaration(declaration.parent as IrClass, context))
                    nameBuilder.append('.')
                }
                if (declaration.dispatchReceiverParameter == null) {
                    nameDeclarator = context.staticContext.rootScope::declareFreshName
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
                    // Return type is only used in signature for inline class types because
                    // they are binary incompatible with supertypes.
                    if (it.isInlined()) {
                        nameBuilder.append("_ret$${it.asString()}")
                    }
                }
            }
        }

        if (nameBuilder.toString() in RESERVED_IDENTIFIERS) {
            nameBuilder.append(0)
            nameDeclarator = context.currentScope::declareFreshName
        }

        return nameDeclarator(sanitizeName(nameBuilder.toString()))
    }
}

private val RESERVED_IDENTIFIERS = setOf(
    // keywords
    "await", "break", "case", "catch", "continue", "debugger", "default", "delete", "do", "else", "finally", "for", "function", "if",
    "in", "instanceof", "new", "return", "switch", "throw", "try", "typeof", "var", "void", "while", "with",

    // future reserved words
    "class", "const", "enum", "export", "extends", "import", "super",

    // as future reserved words in strict mode
    "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield",

    // additional reserved words
    // "null", "true", "false",

    // disallowed as variable names in strict mode
    "eval", "arguments",

    // global identifiers usually declared in a typical JS interpreter
    "NaN", "isNaN", "Infinity", "undefined",

    "Error", "Object", "Number",

    // "Math", "String", "Boolean", "Date", "Array", "RegExp", "JSON",

    // global identifiers usually declared in know environments (node.js, browser, require.js, WebWorkers, etc)
    // "require", "define", "module", "window", "self",

    // the special Kotlin object
    "Kotlin"
)


fun sanitizeName(name: String): String {
    if (name.isEmpty()) return "_"

    val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
    return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
}

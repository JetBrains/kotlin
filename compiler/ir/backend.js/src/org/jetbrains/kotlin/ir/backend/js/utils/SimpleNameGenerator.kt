/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.isDynamic
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

// TODO: this class has to be reimplemented soon
class SimpleNameGenerator : NameGenerator {

    private val nameCache = mutableMapOf<IrDeclaration, JsName>()
    private val loopCache = mutableMapOf<IrLoop, JsName>()

    override fun getNameForSymbol(symbol: IrSymbol, context: JsGenerationContext): JsName =
        if (symbol.isBound) getNameForDeclaration(symbol.owner as IrDeclaration, context) else
            declareDynamic(symbol.descriptor, context)

    override fun getNameForLoop(loop: IrLoop, context: JsGenerationContext): JsName? = loop.label?.let {
        loopCache.getOrPut(loop) { context.currentScope.declareFreshName(sanitizeName(loop.label!!)) }
    }

    override fun getNameForType(type: IrType, context: JsGenerationContext) =
        getNameForDeclaration(type.classifierOrFail.owner as IrDeclaration, context)

    @Deprecated("Descriptors-based code is deprecated")
    private fun declareDynamic(descriptor: DeclarationDescriptor, context: JsGenerationContext): JsName {
        if (descriptor.isDynamic()) {
            return context.currentScope.declareName(descriptor.name.asString())
        }

        if (descriptor is MemberDescriptor && descriptor.isEffectivelyExternal()) {
            val descriptorForName = when (descriptor) {
                is ConstructorDescriptor -> descriptor.constructedClass
                is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                else -> descriptor
            }
            return context.currentScope.declareName(descriptorForName.name.asString())
        }

        throw IllegalStateException("Unbound non-dynamic symbol")
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

    private fun getNameForDeclaration(declaration: IrDeclaration, context: JsGenerationContext): JsName =
        nameCache.getOrPut(declaration) {
            var nameDeclarator: (String) -> JsName = context.currentScope::declareName
            val nameBuilder = StringBuilder()

            val descriptor = declaration.descriptor

            if (declaration.isDynamic()) {
                return@getOrPut nameDeclarator(declaration.descriptor.name.asString())
            }

            if (declaration.isEffectivelyExternal()) {
                // TODO: descriptors are still used here due to the corresponding declaration doesn't have enough information yet
                val descriptorForName = when (descriptor) {
                    is ConstructorDescriptor -> descriptor.constructedClass
                    is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                    else -> descriptor
                }
                return@getOrPut context.staticContext.rootScope.declareName(descriptorForName.name.asString())
            }

            when (declaration) {
                is IrValueParameter -> {
                    if ((context.currentFunction is IrConstructor && declaration.origin == IrDeclarationOrigin.INSTANCE_RECEIVER && declaration.name.isSpecial) ||
                        declaration == context.currentFunction?.dispatchReceiverParameter
                    )
                        nameBuilder.append(Namer.IMPLICIT_RECEIVER_NAME)
                    else if (declaration == context.currentFunction?.extensionReceiverParameter) {
                        nameBuilder.append(Namer.EXTENSION_RECEIVER_NAME)
                    } else {
                        val declaredName = declaration.name.asString()
                        nameBuilder.append(declaredName)
                        if (declaredName.startsWith("\$")) {
                            nameBuilder.append('.')
                            nameBuilder.append(declaration.index)
                        }
                        nameDeclarator = context.currentScope::declareFreshName
                    }
                }
                is IrField -> {
                    nameBuilder.append(declaration.name.asString())
                    if (declaration.parent is IrDeclaration) {
                        nameBuilder.append('.')
                        nameBuilder.append(getNameForDeclaration(declaration.parent as IrDeclaration, context))
                    }
                }
                is IrClass -> {
                    if (declaration.isCompanion) {
                        nameBuilder.append(getNameForDeclaration(declaration.parent as IrDeclaration, context))
                        nameBuilder.append('.')
                    }

                    nameBuilder.append(declaration.name.asString())

                    (declaration.parent as? IrClass)?.let {
                        nameBuilder.append("$")
                        nameBuilder.append(getNameForDeclaration(it, context))
                    }


                    if (declaration.kind == ClassKind.OBJECT || declaration.name.isSpecial || declaration.visibility == Visibilities.LOCAL) {
                        nameDeclarator = context.staticContext.rootScope::declareFreshName
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
                    nameBuilder.append(declaration.name.asString())
                    declaration.extensionReceiverParameter?.let { nameBuilder.append("_\$${it.type.render()}") }
                    declaration.typeParameters.forEach { nameBuilder.append("_${it.name.asString()}") }
                    declaration.valueParameters.forEach { nameBuilder.append("_${it.type.render()}") }
                }

            }

            if (nameBuilder.toString() in RESERVED_IDENTIFIERS) {
                nameBuilder.append(0)
                nameDeclarator = context.currentScope::declareFreshName
            }

            nameDeclarator(sanitizeName(nameBuilder.toString()))
        }


    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
        return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
    }
}
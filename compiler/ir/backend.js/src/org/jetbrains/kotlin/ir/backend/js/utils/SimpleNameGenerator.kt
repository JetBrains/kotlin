/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class SimpleNameGenerator : NameGenerator {

    private val nameCache = mutableMapOf<DeclarationDescriptor, JsName>()
    private val loopCache = mutableMapOf<IrLoop, JsName>()

    override fun getNameForSymbol(symbol: IrSymbol, context: JsGenerationContext): JsName = getNameForDescriptor(symbol.descriptor, context)
    override fun getNameForLoop(loop: IrLoop, context: JsGenerationContext): JsName? = loop.label?.let {
        loopCache.getOrPut(loop) { context.currentScope.declareFreshName(sanitizeName(loop.label!!)) }
    }


    private fun getNameForDescriptor(descriptor: DeclarationDescriptor, context: JsGenerationContext): JsName =
        nameCache.getOrPut(descriptor) {
            var nameDeclarator: (String) -> JsName = context.currentScope::declareName

            if (descriptor.isDynamic()) {
                return@getOrPut nameDeclarator(descriptor.name.asString())
            }

            if (descriptor is MemberDescriptor && descriptor.isEffectivelyExternal()) {
                val descriptorForName = when (descriptor) {
                    is ConstructorDescriptor -> descriptor.constructedClass
                    is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                    else -> descriptor
                }
                return@getOrPut nameDeclarator(descriptorForName.name.asString())
            }

            val nameBuilder = StringBuilder()
            when (descriptor) {
                is ReceiverParameterDescriptor -> {
                    when (descriptor.value) {
                        is ExtensionReceiver -> nameBuilder.append(Namer.EXTENSION_RECEIVER_NAME)
                        is ImplicitClassReceiver -> nameBuilder.append(Namer.IMPLICIT_RECEIVER_NAME)
                        else -> TODO("name for $descriptor")
                    }
                }
                is ValueParameterDescriptor -> {
                    val declaredName = descriptor.name.asString()
                    nameBuilder.append(declaredName)
                    nameDeclarator = context.currentScope::declareFreshName
                }
                is PropertyDescriptor -> {
                    nameBuilder.append(descriptor.name.asString())
                    if (descriptor.visibility == Visibilities.PRIVATE || descriptor.modality != Modality.FINAL) {
                        nameBuilder.append('$')
                        nameBuilder.append(getNameForDescriptor(descriptor.containingDeclaration, context))
                    }
                }
                is PropertyAccessorDescriptor -> {
                    when (descriptor) {
                        is PropertyGetterDescriptor -> nameBuilder.append(Namer.GETTER_PREFIX)
                        is PropertySetterDescriptor -> nameBuilder.append(Namer.SETTER_PREFIX)
                    }

                    nameBuilder.append(descriptor.correspondingProperty.name.asString())

                    descriptor.extensionReceiverParameter?.let {
                        nameBuilder.append("_r$${it.type}")
                    }

                    if (descriptor.visibility == Visibilities.PRIVATE) {
                        nameBuilder.append('$')
                        nameBuilder.append(getNameForDescriptor(descriptor.containingDeclaration, context))
                    }
                }
                is ClassDescriptor -> {
                    if (descriptor.name.isSpecial) {
                        nameBuilder.append(descriptor.name.asString().let {
                            it.substring(1, it.length - 1) + "${descriptor.hashCode()}"
                        })
                    } else {
                        nameBuilder.append(descriptor.fqNameSafe.asString().replace('.', '$'))
                    }
                }
                is ConstructorDescriptor -> {
                    nameBuilder.append(getNameForDescriptor(descriptor.constructedClass, context))
                }
                is VariableDescriptor -> {
                    nameBuilder.append(descriptor.name.identifier)
                    nameDeclarator = context.currentScope::declareFreshName
                }
                is CallableDescriptor -> {
                    nameBuilder.append(descriptor.name.asString())
                    descriptor.typeParameters.ifNotEmpty {
                        nameBuilder.append("_\$t")
                        joinTo(nameBuilder, "") { "_${it.name}" }
                    }
                    descriptor.extensionReceiverParameter?.let {
                        nameBuilder.append("_r$${it.type}")
                    }
                    descriptor.valueParameters.ifNotEmpty {
                        joinTo(nameBuilder, "") { "_${it.type}" }
                    }
                }

            }
            nameDeclarator(sanitizeName(nameBuilder.toString()))
        }

    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
        return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
    }
}
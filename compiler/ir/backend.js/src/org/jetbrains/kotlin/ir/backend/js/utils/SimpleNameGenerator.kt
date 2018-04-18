/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver

class SimpleNameGenerator : NameGenerator {

    private val nameCache = mutableMapOf<DeclarationDescriptor, JsName>()

    override fun getNameForSymbol(symbol: IrSymbol, context: JsGenerationContext): JsName = getNameForDescriptor(symbol.descriptor, context)

    private fun getNameForDescriptor(descriptor: DeclarationDescriptor, context: JsGenerationContext): JsName =
        nameCache.getOrPut(descriptor) {
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
                    if (descriptor.name.isSpecial) {
                        // TODO: consider this case more carefully
                        nameBuilder.append(Namer.IMPLICIT_RECEIVER_NAME)
                        nameBuilder.append('_')
                        nameBuilder.append(descriptor.index)
                    } else {
                        val declaredName = descriptor.name.identifier
                        nameBuilder.append(declaredName)
                        if (declaredName.startsWith("\$")) {
                            nameBuilder.append('_')
                            nameBuilder.append(descriptor.index)
                        }
                    }
                }
                is PropertyDescriptor -> {
                    nameBuilder.append(descriptor.name.identifier)
                }
                is PropertyAccessorDescriptor -> {
                    when (descriptor) {
                        is PropertyGetterDescriptor -> nameBuilder.append(Namer.GETTER_PREFIX)
                        is PropertySetterDescriptor -> nameBuilder.append(Namer.SETTER_PREFIX)
                    }
                    nameBuilder.append(descriptor.correspondingProperty.name.asString())
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
                is LocalVariableDescriptor -> {
                    nameBuilder.append(descriptor.name.identifier)
                }
                is CallableDescriptor -> {
                    nameBuilder.append(descriptor.name.asString())
                    descriptor.typeParameters.forEach { nameBuilder.append("_${it.name.asString()}") }
                    descriptor.valueParameters.forEach { nameBuilder.append("_${it.type}") }
                }

            }
            context.currentScope.declareName(sanitizeName(nameBuilder.toString()))
        }

    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
        return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
    }
}
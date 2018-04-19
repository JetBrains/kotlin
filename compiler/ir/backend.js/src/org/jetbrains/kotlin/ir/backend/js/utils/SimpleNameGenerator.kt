/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.naming.isES5IdentifierPart
import org.jetbrains.kotlin.js.naming.isES5IdentifierStart
import org.jetbrains.kotlin.name.Name

class SimpleNameGenerator : NameGenerator {

    private val nameCache = mutableMapOf<DeclarationDescriptor, JsName>()

    override fun getNameForSymbol(symbol: IrSymbol, scope: JsScope): JsName = getNameForDescriptor(symbol.descriptor, scope)

    override fun getSpecialRefForName(name: Name): JsExpression {
        assert(name.isSpecial)

        val nameString = name.asString()
        return when (nameString) {
            Namer.THIS_SPECIAL_NAME -> JsThisRef()
            else -> JsNameRef(getSpecialNameString(nameString))
        }
    }

    override fun getSpecialNameString(specNameString: String): String = when (specNameString) {
        Namer.SET_SPECIAL_NAME -> Namer.SETTER_ARGUMENT
        else -> TODO("for Name ${specNameString}")
    }

    private fun getNameForDescriptor(descriptor: DeclarationDescriptor, scope: JsScope): JsName = nameCache.getOrPut(descriptor, {
        val nameBuilder = StringBuilder()
        when (descriptor) {
            is PropertyAccessorDescriptor -> {
                when (descriptor) {
                    is PropertyGetterDescriptor -> nameBuilder.append(Namer.GETTER_PREFIX)
                    is PropertySetterDescriptor -> nameBuilder.append(Namer.SETTER_PREFIX)
                }
                nameBuilder.append(descriptor.correspondingProperty.name.asString())
            }
            is CallableDescriptor -> {
                nameBuilder.append(descriptor.name.asString())
                descriptor.typeParameters.forEach { nameBuilder.append("_${it.name.asString()}") }
                descriptor.valueParameters.forEach { nameBuilder.append("_${it.type}") }
            }

        }
        scope.declareName(sanitizeName(nameBuilder.toString()))
    })

    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
        return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
    }
}
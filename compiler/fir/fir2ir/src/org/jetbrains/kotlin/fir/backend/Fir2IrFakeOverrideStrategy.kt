/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.declarations.IrPropertyWithLateBinding
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.util.render

class Fir2IrFakeOverrideStrategy(
    friendModules: Map<String, List<String>>,
    override val isGenericClashFromSameSupertypeAllowed: Boolean,
    override val isOverrideOfPublishedApiFromOtherModuleDisallowed: Boolean,
) : FakeOverrideBuilderStrategy.BindToPrivateSymbols(friendModules) {
    private val fieldOnlyProperties: MutableList<IrPropertyWithLateBinding> = mutableListOf()

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
        super.linkPropertyFakeOverride(property, manglerCompatibleMode)

        if (property.getter == null) {
            fieldOnlyProperties.add(property)
        }
    }

    fun clearFakeOverrideFields() {
        for (property in fieldOnlyProperties) {
            check(property.isFakeOverride && property.getter == null) { "Not a field-only property: " + property.render() }
            property.backingField = null
        }
    }
}

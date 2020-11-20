/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor

object ClassicBuiltinSpecialProperties {
    fun CallableMemberDescriptor.getBuiltinSpecialPropertyGetterName(): String? {
        assert(KotlinBuiltIns.isBuiltIn(this)) { "This method is defined only for builtin members, but $this found" }

        val descriptor = propertyIfAccessor.firstOverridden { hasBuiltinSpecialPropertyFqName(it) } ?: return null
        return BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[descriptor.fqNameSafe]?.asString()
    }

    fun hasBuiltinSpecialPropertyFqName(callableMemberDescriptor: CallableMemberDescriptor): Boolean {
        if (callableMemberDescriptor.name !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return false

        return callableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl()
    }

    private fun CallableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
        if (fqNameOrNull() in BuiltinSpecialProperties.SPECIAL_FQ_NAMES && valueParameters.isEmpty()) return true
        if (!KotlinBuiltIns.isBuiltIn(this)) return false

        return overriddenDescriptors.any { hasBuiltinSpecialPropertyFqName(it) }
    }
}

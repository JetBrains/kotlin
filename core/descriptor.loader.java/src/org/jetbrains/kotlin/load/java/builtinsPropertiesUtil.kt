/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

private val BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES = setOf(FqName("kotlin.Collection.size"), FqName("kotlin.Map.size"))
private val BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES = BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES.map { it.shortName() }.toSet()

public fun CallableDescriptor.hasBuiltinSpecialPropertyFqName(): Boolean {
    if (this is PropertyAccessorDescriptor) return correspondingProperty.hasBuiltinSpecialPropertyFqName()
    if (name !in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES) return false

    return hasBuiltinSpecialPropertyFqNameImpl()
}

private fun CallableDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
    if (fqNameUnsafe.check { it.isSafe }?.toSafe() in BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES) return true

    if (!fqNameUnsafe.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return false
    if (builtIns.builtInsModule != module) return false

    return overriddenDescriptors.any(CallableDescriptor::hasBuiltinSpecialPropertyFqName)
}

val Name.isBuiltinSpecialPropertyName: Boolean get() = this in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES

private val CallableDescriptor.builtinSpecialPropertyAccessorName: String?
    get() = when(this) {
        is PropertyAccessorDescriptor -> correspondingProperty.check { it.hasBuiltinSpecialPropertyFqName() }?.name?.asString()
        else -> null
    }

@Suppress("UNCHECKED_CAST")
val <T : CallableDescriptor> T.builtinSpecialOverridden: T? get() {
    return when (this) {
        is PropertyAccessorDescriptor -> check { correspondingProperty.hasBuiltinSpecialPropertyFqName() }
                                         ?: overriddenDescriptors.firstNotNullResult { it.builtinSpecialOverridden } as T?
        is PropertyDescriptor -> check { hasBuiltinSpecialPropertyFqName() }
                                 ?: overriddenDescriptors.firstNotNullResult { it.builtinSpecialOverridden } as T?
        else -> null
    }
}

fun CallableDescriptor.overridesBuiltinSpecialDeclaration(): Boolean = builtinSpecialOverridden != null

public val CallableDescriptor.jvmMethodNameIfSpecial: String?
    get() = builtinOverriddenThatAffectsJvmName?.builtinSpecialPropertyAccessorName

public val CallableDescriptor.builtinOverriddenThatAffectsJvmName: CallableDescriptor?
    get() = if (hasBuiltinSpecialPropertyFqName() || isFromJava) builtinSpecialOverridden else null

private val CallableDescriptor.isFromJava: Boolean
    get() = propertyIfAccessor is JavaCallableMemberDescriptor

private val CallableDescriptor.propertyIfAccessor: CallableDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_SHORT

enum class JvmFieldApplicabilityProblem(val errorMessage: String) {
    NOT_FINAL("JvmField can only be applied to final property"),
    PRIVATE("JvmField has no effect on a private property"),
    CUSTOM_ACCESSOR("JvmField cannot be applied to a property with a custom accessor"),
    OVERRIDES("JvmField cannot be applied to a property that overrides some other property"),
    LATEINIT("JvmField cannot be applied to lateinit property"),
    CONST("JvmField cannot be applied to const property"),
    INSIDE_COMPANION_OF_INTERFACE("JvmField cannot be applied to a property defined in companion object of interface"),
    NOT_PUBLIC_VAL_WITH_JVMFIELD("JvmField could be applied only if all interface companion properties are 'public final val' with '@JvmField' annotation"),
    TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE("JvmField cannot be applied to top level property of a file annotated with ${JVM_MULTIFILE_CLASS_SHORT}"),
    DELEGATE("JvmField cannot be applied to delegated property"),
    RETURN_TYPE_IS_VALUE_CLASS("JvmField cannot be applied to a property of a value class type"),
}
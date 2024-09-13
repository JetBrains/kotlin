/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val NO_INFER_ANNOTATION_FQ_NAME = FqName("kotlin.internal.NoInfer")
val EXACT_ANNOTATION_FQ_NAME = FqName("kotlin.internal.Exact")
val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME = FqName("kotlin.internal.LowPriorityInOverloadResolution")
val HIDES_MEMBERS_ANNOTATION_FQ_NAME = FqName("kotlin.internal.HidesMembers")
val ONLY_INPUT_TYPES_FQ_NAME = FqName("kotlin.internal.OnlyInputTypes")
val DYNAMIC_EXTENSION_FQ_NAME = FqName("kotlin.internal.DynamicExtension")
val BUILDER_INFERENCE_ANNOTATION_FQ_NAME = FqName("kotlin.BuilderInference")

// @HidesMembers annotation only has effect for members with these names
val HIDES_MEMBERS_NAME_LIST = setOf(Name.identifier("forEach"), Name.identifier("addSuppressed"))

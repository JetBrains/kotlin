/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.kdoc

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils

private val boringBuiltinClasses = setOf(
        FQ_NAMES.unit,
        FQ_NAMES._byte,
        FQ_NAMES._short,
        FQ_NAMES._int,
        FQ_NAMES._long,
        FQ_NAMES._char,
        FQ_NAMES._boolean,
        FQ_NAMES._float,
        FQ_NAMES._double,
        FQ_NAMES.string,
        FQ_NAMES.array,
        FQ_NAMES.any
)

fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean =
        DescriptorUtils.getFqName(this) in boringBuiltinClasses
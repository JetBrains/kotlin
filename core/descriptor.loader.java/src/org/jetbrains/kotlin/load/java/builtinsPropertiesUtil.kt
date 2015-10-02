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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

private val BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES = setOf(FqName("kotlin.Collection.size"), FqName("kotlin.Map.size"))
private val BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES = BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES.map { it.shortName() }.toSet()

private fun DeclarationDescriptor.hasBuiltinSpecialPropertyFqName()
    = name in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES
        && fqNameUnsafe.check { it.isSafe }?.toSafe() in BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES

val Name.isBuiltinSpecialPropertyName: Boolean get() = this in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES

val PropertyDescriptor.builtinSpecialOverridden: PropertyDescriptor?
    get() = check { hasBuiltinSpecialPropertyFqName() } ?: overriddenDescriptors.firstNotNullResult { it.builtinSpecialOverridden }

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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

data class KotlinTypeInfo(val isCovariant: Boolean, val type: KotlinType? = null, val text: String? = null)

fun KotlinTypeInfo.render(): String {
    return when {
        text != null -> text
        type != null -> (if (isCovariant) IdeDescriptorRenderers.SOURCE_CODE else IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION).renderType(type)
        else -> ""
    }
}

fun KotlinTypeInfo.isEquivalentTo(other: KotlinTypeInfo): Boolean {
    return if (type != null && other.type != null) TypeUtils.equalTypes(type, other.type) else render() == other.render()
}
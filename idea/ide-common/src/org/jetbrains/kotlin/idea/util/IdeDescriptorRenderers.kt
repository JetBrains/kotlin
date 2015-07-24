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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.isDynamic

public object IdeDescriptorRenderers {
    public val APPROXIMATE_FLEXIBLE_TYPES: (JetType) -> JetType = { approximateFlexibleTypes(it, true) }

    public val APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS: (JetType) -> JetType = { approximateFlexibleTypes(it, false) }

    private fun unwrapAnonymousType(type: JetType): JetType {
        if (type.isDynamic()) return type

        val classifier = type.constructor.declarationDescriptor
        if (classifier != null && !classifier.name.isSpecial) return type

        type.constructor.supertypes.singleOrNull()?.let { return it }

        val builtIns = KotlinBuiltIns.getInstance()
        return if (type.isMarkedNullable)
            builtIns.nullableAnyType
        else
            builtIns.anyType
    }

    private val BASE: DescriptorRenderer = DescriptorRenderer.withOptions {
        normalizedVisibilities = true
        withDefinedIn = false
        showInternalKeyword = false
        overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
        unitReturnType = false
        modifiers = DescriptorRendererModifier.ALL
    }

    public val SOURCE_CODE: DescriptorRenderer = BASE.withOptions {
        nameShortness = NameShortness.SOURCE_CODE_QUALIFIED
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
    }

    public val SOURCE_CODE_FOR_TYPE_ARGUMENTS: DescriptorRenderer = BASE.withOptions {
        nameShortness = NameShortness.SOURCE_CODE_QUALIFIED
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS(unwrapAnonymousType(it)) }
    }

    public val SOURCE_CODE_SHORT_NAMES_IN_TYPES: DescriptorRenderer = BASE.withOptions {
        nameShortness = NameShortness.SHORT
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
    }
}

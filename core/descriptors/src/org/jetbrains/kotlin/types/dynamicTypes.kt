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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.DynamicTypeMarker
import org.jetbrains.kotlin.types.typeUtil.builtIns

@DefaultImplementation(impl = DynamicTypesSettings::class)
open class DynamicTypesSettings : PlatformSpecificExtension<DynamicTypesSettings> {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed : DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

fun KotlinType.isDynamic(): Boolean = unwrap() is DynamicType

fun createDynamicType(builtIns: KotlinBuiltIns) = DynamicType(builtIns, Annotations.EMPTY)

class DynamicType(
    builtIns: KotlinBuiltIns,
    override val annotations: Annotations
) : FlexibleType(builtIns.nothingType, builtIns.nullableAnyType), DynamicTypeMarker {
    override val delegate: SimpleType get() = upperBound

    // Nullability has no effect on dynamics
    override fun makeNullableAsSpecified(newNullability: Boolean): DynamicType = this

    override val isMarkedNullable: Boolean get() = false

    override fun replaceAnnotations(newAnnotations: Annotations): DynamicType = DynamicType(delegate.builtIns, newAnnotations)

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String = "dynamic"

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this
}

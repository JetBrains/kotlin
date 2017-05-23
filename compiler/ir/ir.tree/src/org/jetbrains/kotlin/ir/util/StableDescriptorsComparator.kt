/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import java.util.*

object StableDescriptorsComparator : Comparator<DeclarationDescriptor> {
    override fun compare(member1: DeclarationDescriptor?, member2: DeclarationDescriptor?): Int {
        if (member1 == member2) return 0
        if (member1 == null) return -1
        if (member2 == null) return 1

        val image1 = DESCRIPTOR_RENDERER.render(member1)
        val image2 = DESCRIPTOR_RENDERER.render(member2)
        return image1.compareTo(image2)
    }

    private val DESCRIPTOR_RENDERER = DescriptorRenderer.withOptions {
        withDefinedIn = false
        overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
        includePropertyConstant = true
        classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
        verbose = true
        modifiers = DescriptorRendererModifier.ALL
    }
}
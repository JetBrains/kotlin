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

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaModifierListOwner
import java.lang.reflect.Modifier

interface ReflectJavaModifierListOwner : JavaModifierListOwner {
    val modifiers: Int

    override val isAbstract: Boolean
        get() = Modifier.isAbstract(modifiers)

    override val isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

    override val isFinal: Boolean
        get() = Modifier.isFinal(modifiers)

    override val visibility: DescriptorVisibility
        get() = modifiers.let { modifiers ->
            when {
                Modifier.isPublic(modifiers) -> DescriptorVisibilities.PUBLIC
                Modifier.isPrivate(modifiers) -> DescriptorVisibilities.PRIVATE
                Modifier.isProtected(modifiers) ->
                    if (Modifier.isStatic(modifiers)) JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
                    else JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE
                else -> JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            }
        }
}

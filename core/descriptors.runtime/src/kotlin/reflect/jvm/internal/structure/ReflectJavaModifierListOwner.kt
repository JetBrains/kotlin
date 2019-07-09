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

package kotlin.reflect.jvm.internal.structure

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.JavaVisibilities
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

    override val visibility: Visibility
        get() = modifiers.let { modifiers ->
            when {
                Modifier.isPublic(modifiers) -> Visibilities.PUBLIC
                Modifier.isPrivate(modifiers) -> Visibilities.PRIVATE
                Modifier.isProtected(modifiers) ->
                    if (Modifier.isStatic(modifiers)) JavaVisibilities.PROTECTED_STATIC_VISIBILITY
                    else JavaVisibilities.PROTECTED_AND_PACKAGE
                else -> JavaVisibilities.PACKAGE_VISIBILITY
            }
        }
}

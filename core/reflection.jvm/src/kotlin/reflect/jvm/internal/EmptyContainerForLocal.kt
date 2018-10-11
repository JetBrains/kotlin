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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KCallable

internal object EmptyContainerForLocal : KDeclarationContainerImpl() {
    override val jClass: Class<*>
        get() = fail()

    override val members: Collection<KCallable<*>>
        get() = fail()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = fail()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> = fail()

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = fail()

    override fun getLocalProperty(index: Int): PropertyDescriptor? = null

    private fun fail(): Nothing = throw KotlinReflectionInternalError(
        "Introspecting local functions, lambdas, anonymous functions and local variables is not yet fully supported in Kotlin reflection"
    )
}

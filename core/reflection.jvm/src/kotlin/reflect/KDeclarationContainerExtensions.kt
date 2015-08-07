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

package kotlin.reflect

import kotlin.reflect.jvm.internal.KDeclarationContainerImpl

/**
 * Returns all functions declared in this container.
 * If this container is a Java class, it includes all non-static methods declared in the class
 * and the superclasses, as well as static methods declared in the class.
 */
public val KDeclarationContainer.functions: Collection<KFunction<*>>
    get() = (this as KDeclarationContainerImpl)
            .getMembers(declaredOnly = false, nonExtensions = true, extensions = true)
            .filterIsInstance<KFunction<*>>()
            .toList()

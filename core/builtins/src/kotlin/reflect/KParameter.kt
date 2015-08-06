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

/**
 * Represents a parameter passed to a function or a property getter/setter,
 * including `this` and extension receiver parameters.
 */
public interface KParameter : KAnnotatedElement {
    /**
     * 0-based index of this parameter in the parameter list of its containing callable.
     */
    public val index: Int

    /**
     * Name of this parameter as it was declared in the source code,
     * or `null` if the parameter has no name or its name is not available at runtime.
     * Examples of nameless parameters include `this` instance for member functions,
     * extension receiver for extension functions or properties, parameters of Java methods
     * compiled without the debug information, and others.
     */
    public val name: String?

    /**
     * Type of this parameter. For a `vararg` parameter, this is the type of the corresponding array,
     * not the individual element.
     */
    public val type: KType
}

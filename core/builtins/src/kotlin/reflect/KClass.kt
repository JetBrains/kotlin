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
 * Represents a class and provides introspection capabilities.
 * Instances of this class are obtainable by the `::class` syntax.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/reflection.html#class-references)
 * for more information.
 *
 * @param T the type of the class.
 */
public interface KClass<T> {
    /**
     * The simple name of the class as it was declared in the source code,
     * or `null` if the class has no name (e.g. anonymous object literals).
     */
    public val simpleName: String?

    /**
     * Returns non-extension properties declared in this class and all of its superclasses.
     */
    public val properties: Collection<KMemberProperty<T, *>>

    /**
     * Returns extension properties declared in this class and all of its superclasses.
     */
    public val extensionProperties: Collection<KMemberExtensionProperty<T, *, *>>
}

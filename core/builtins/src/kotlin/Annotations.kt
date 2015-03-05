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

package kotlin

/**
 * Marks the annotated class as a data class. The compiler automatically generates
 * equals()/hashCode(), toString(), componentN() and copy() functions for data classes.
 * See [the Kotlin language documentation](http://kotlinlang.org/docs/reference/data-classes.html)
 * for more information.
 */
public annotation class data

/**
 * Marks the annotated class, function or property as deprecated.
 * @property value the message explaining the deprecation and recommending an alternative API to use.
 */
public annotation class deprecated(val value: String)

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
public annotation class suppress(vararg val names: String)

/**
 * Enables the tail call optimization for the annotated function. If the annotated function
 * calls itself recursively as the last operation it performs, it will be executed without
 * growing the stack depth. Tail call optimization is currently only supported by the JVM
 * backend.
 */
public annotation class tailRecursive

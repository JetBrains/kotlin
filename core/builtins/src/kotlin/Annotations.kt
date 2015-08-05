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

import kotlin.annotation.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.annotation.AnnotationRetention.*

/**
 * Marks the annotated class as a data class. The compiler automatically generates
 * equals()/hashCode(), toString(), componentN() and copy() functions for data classes.
 * See [the Kotlin language documentation](http://kotlinlang.org/docs/reference/data-classes.html)
 * for more information.
 */
target(CLASSIFIER)
public annotation(mustBeDocumented = true) class data

/**
 * Marks the annotated class, function, property, variable or parameter as deprecated.
 * @property value the message explaining the deprecation and recommending an alternative API to use.
 * @property replaceWith if present, specifies a code fragment which should be used as a replacement for
 *     the deprecated API usage.
 */
target(CLASSIFIER, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER,
       LOCAL_VARIABLE, FIELD, VALUE_PARAMETER)
public annotation(mustBeDocumented = true) class deprecated(val value: String, val replaceWith: ReplaceWith = ReplaceWith(""))

/**
 * Specifies a code fragment that can be used to replace a deprecated function or property. Tools such
 * as IDEs can automatically apply the replacements specified through this annotation.
 *
 * @property expression the replacement expression. The replacement expression is interpreted in the context
 *     of the function or property being called, and can reference members of enclosing classes etc.
 *     For function calls, the replacement expression may contain argument names of the deprecated function,
 *     which will be substituted with actual parameters used in the call being updated. The imports used in the file
 *     containing the deprecated function or property are NOT accessible; if the replacement expression refers
 *     on any of those imports, they need to be specified explicitly in the [imports] parmeter.
 * @property imports the qualified names that need to be imported in order for the references in the
 *     replacement expression to be resolved correctly.
 */
target()
public annotation(retention = BINARY, mustBeDocumented = true) class ReplaceWith(val expression: String, vararg val imports: String)

/**
 * Signifies that the annotated functional type represents an extension function.
 */
target(TYPE)
public annotation(mustBeDocumented = true) class extension

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
target(CLASSIFIER, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
       CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE)
public annotation(retention = SOURCE) class suppress(vararg val names: String)

/**
 * Enables the tail call optimization for the annotated function. If the annotated function
 * calls itself recursively as the last operation it performs, it will be executed without
 * growing the stack depth. Tail call optimization is currently only supported by the JVM
 * backend.
 */
target(FUNCTION)
public annotation(retention = SOURCE) class tailRecursive

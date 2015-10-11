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

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks the annotated class as a data class. The compiler automatically generates
 * equals()/hashCode(), toString(), componentN() and copy() functions for data classes.
 * See [the Kotlin language documentation](http://kotlinlang.org/docs/reference/data-classes.html)
 * for more information.
 */
@Target(CLASS)
@MustBeDocumented
private annotation class data

/**
 * Marks the annotated class, function, property, variable or parameter as deprecated.
 * @property message the message explaining the deprecation and recommending an alternative API to use.
 * @property replaceWith if present, specifies a code fragment which should be used as a replacement for
 *     the deprecated API usage.
 */
@Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER)
@MustBeDocumented
public annotation class Deprecated(
        val message: String,
        val replaceWith: ReplaceWith = ReplaceWith(""),
        val level: DeprecationLevel = DeprecationLevel.WARNING
)

/**
 * Specifies a code fragment that can be used to replace a deprecated function, property or class. Tools such
 * as IDEs can automatically apply the replacements specified through this annotation.
 *
 * @property expression the replacement expression. The replacement expression is interpreted in the context
 *     of the symbol being used, and can reference members of enclosing classes etc.
 *     For function calls, the replacement expression may contain argument names of the deprecated function,
 *     which will be substituted with actual parameters used in the call being updated. The imports used in the file
 *     containing the deprecated function or property are NOT accessible; if the replacement expression refers
 *     on any of those imports, they need to be specified explicitly in the [imports] parameter.
 * @property imports the qualified names that need to be imported in order for the references in the
 *     replacement expression to be resolved correctly.
 */
@Target()
@Retention(BINARY)
@MustBeDocumented
public annotation class ReplaceWith(val expression: String, vararg val imports: String)

/**
 * Contains levels for deprecation levels.
 */
public enum class DeprecationLevel {
    /** Usage of the deprecated element will be marked as a warning. */
    WARNING,
    /** Usage of the deprecated element will be marked as an error. */
    ERROR,
    /** Deprecated element will not be accessible from code. */
    HIDDEN
}

/**
 * Signifies that the annotated functional type represents an extension function.
 */
@Target(TYPE)
@MustBeDocumented
public annotation class Extension

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
        CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE)
@Retention(SOURCE)
public annotation class Suppress(vararg val names: String)

/**
 * Enables the tail call optimization for the annotated function. If the annotated function
 * calls itself recursively as the last operation it performs, it will be executed without
 * growing the stack depth. Tail call optimization is currently only supported by the JVM
 * backend.
 */
@Target(FUNCTION)
@Retention(SOURCE)
private annotation class tailrec

/**
 * Hides the annotated function, property or constructor from the overload resolution,
 * thus preventing its usages from newly compiled code, but keeps compiling it
 * to retain binary compatibility with the code compiled against it before.
 */
@Target(FUNCTION, PROPERTY, CONSTRUCTOR)
@Retention(BINARY)
@MustBeDocumented
public annotation class HiddenDeclaration

/**
 * Marks annotated function as `external`, meaning that it's not implemented
 * in Kotlin but rather in a different language (for example, in C/C++ using JNI or JavaScript).
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(SOURCE)
@MustBeDocumented
private annotation class external

/**
 * Suppresses errors about variance conflict
 */
@Target(TYPE)
@Retention(SOURCE)
@MustBeDocumented
public annotation class UnsafeVariance

/**
 * Specifies that the corresponding type should be ignored during type inference.
 */
@Target(TYPE)
@Retention(SOURCE)
internal annotation class NoInfer

/**
 * Specifies that the constraint built for the type during type inference should be an equality one.
 */
@Target(TYPE)
@Retention(SOURCE)
internal annotation class Exact
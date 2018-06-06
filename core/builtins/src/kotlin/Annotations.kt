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
 * Marks the annotated class, function, property, variable or parameter as deprecated.
 * @property message the message explaining the deprecation and recommending an alternative API to use.
 * @property replaceWith if present, specifies a code fragment which should be used as a replacement for
 *     the deprecated API usage.
 */
@Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER, TYPEALIAS)
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
public annotation class ExtensionFunctionType

/**
 * Annotates type arguments of functional type and holds corresponding parameter name specified by the user in type declaration (if any).
 */
@Target(TYPE)
@MustBeDocumented
@SinceKotlin("1.1")
public annotation class ParameterName(val name: String)

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
        CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@Retention(SOURCE)
public annotation class Suppress(vararg val names: String)

/**
 * Suppresses errors about variance conflict
 */
@Target(TYPE)
@Retention(SOURCE)
@MustBeDocumented
public annotation class UnsafeVariance

/**
 * Specifies the first version of Kotlin where a declaration has appeared.
 * Using the declaration and specifying an older API version (via the `-api-version` command line option) will result in an error.
 *
 * @property version the version in the following formats: `<major>.<minor>` or `<major>.<minor>.<patch>`, where major, minor and patch
 * are non-negative integer numbers without leading zeros.
 */
@Target(CLASS, PROPERTY, FIELD, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class SinceKotlin(val version: String)

/**
 * When applied to annotation class X specifies that X defines a DSL language
 *
 * The general rule:
 * - an implicit receiver may *belong to a DSL @X* if marked with a corresponding DSL marker annotation
 * - two implicit receivers of the same DSL are not accessible in the same scope
 * - the closest one wins
 * - other available receivers are resolved as usual, but if the resulting resolved call binds to such a receiver, it's a compilation error
 *
 * Marking rules: an implicit receiver is considered marked with @Ann if
 * - its type is marked, or
 * - its type's classifier is marked
 * - or any of its superclasses/superinterfaces
 */
@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@MustBeDocumented
@SinceKotlin("1.1")
public annotation class DslMarker


/**
 * When applied to a class or a member with internal visibility allows to use it from public inline functions and
 * makes it effectively public.
 *
 * Public inline functions cannot use non-public API, since if they are inlined, those non-public API references
 * would violate access restrictions at a call site (http://kotlinlang.org/docs/reference/inline-functions.html#public-inline-restrictions).
 *
 * To overcome this restriction an `internal` declaration can be annotated with the `@PublishedApi` annotation:
 * - this allows to call that declaration from public inline functions;
 * - the declaration becomes effectively public, and this should be considered with respect to binary compatibility maintaining.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("1.1")
public annotation class PublishedApi
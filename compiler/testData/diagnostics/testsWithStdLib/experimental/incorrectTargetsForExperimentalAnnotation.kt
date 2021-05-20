// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

import kotlin.annotation.AnnotationTarget.*

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION,
        PROPERTY_SETTER, TYPE, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
annotation class E1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE)<!>
annotation class E2

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(EXPRESSION)<!>
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_RETENTION!>@Retention(AnnotationRetention.SOURCE)<!>
annotation class E3

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class E4

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
annotation class E5

var some: Int
    <!EXPERIMENTAL_ANNOTATION_ON_GETTER!>@E4<!>
    get() = 42
    @E5
    set(value) {}

<!EXPERIMENTAL_ANNOTATION_ON_GETTER!>@get:E4<!>
val another: Int = 42


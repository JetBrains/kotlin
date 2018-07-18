// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

import kotlin.annotation.AnnotationTarget.*

@Experimental(Experimental.Level.WARNING)
@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION,
        PROPERTY_GETTER, PROPERTY_SETTER, TYPE, TYPEALIAS)
annotation class E1

@Experimental(Experimental.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE)<!>
annotation class E2

@Experimental(Experimental.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(EXPRESSION)<!>
@Retention(AnnotationRetention.SOURCE)
annotation class E3

@Experimental(Experimental.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE, EXPRESSION)<!>
@Retention(AnnotationRetention.SOURCE)
annotation class E4

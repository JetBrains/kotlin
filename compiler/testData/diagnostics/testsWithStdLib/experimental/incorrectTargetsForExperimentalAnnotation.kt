// !API_VERSION: 1.3
// FILE: api.kt

package api

import kotlin.annotation.AnnotationTarget.*

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
@Target(CLASS, ANNOTATION_CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION,
        PROPERTY_GETTER, PROPERTY_SETTER, TYPE, TYPEALIAS)
annotation class E1

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE)<!>
annotation class E2

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(EXPRESSION)<!>
annotation class E3

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.RUNTIME])
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE, EXPRESSION)<!>
annotation class E4

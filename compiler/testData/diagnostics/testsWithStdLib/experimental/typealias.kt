// !API_VERSION: 1.3
// FILE: api.kt

package api

@Experimental
@Target(AnnotationTarget.CLASS)
annotation class ExperimentalAPI

@ExperimentalAPI
class Foo

typealias Bar = <!EXPERIMENTAL_API_USAGE_ERROR!>Foo<!>

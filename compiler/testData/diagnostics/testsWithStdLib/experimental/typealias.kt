// FIR_IDENTICAL
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
class Foo

typealias Bar = <!EXPERIMENTAL_API_USAGE_ERROR!>Foo<!>

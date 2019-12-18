// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn
@Target(AnnotationTarget.CLASS)
annotation class ExperimentalAPI

@ExperimentalAPI
class Foo

typealias Bar = Foo

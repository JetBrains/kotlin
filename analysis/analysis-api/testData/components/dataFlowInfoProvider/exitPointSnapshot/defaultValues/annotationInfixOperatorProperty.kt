@file:Suppress("ANNOTATION_ARGUMENT_MUST_BE_CONST")
package test

annotation class Anno(val value: String)

@Anno(value = "A" <expr>foo</expr> "B")
class Foo

val foo: String.(String) -> String = { it }
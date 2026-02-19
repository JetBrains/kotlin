// WITH_STDLIB

@file:Suppress("ANNOTATION_ARGUMENT_MUST_BE_CONST")
package test

annotation class Anno(val value: String)

val flag: Boolean
    get() = true

@Anno(value = buildString {
    <expr>append("Foo")
    if (flag) {
        append("Bar")
        return@buildString
    }
    append("Baz")</expr>
})
class Foo
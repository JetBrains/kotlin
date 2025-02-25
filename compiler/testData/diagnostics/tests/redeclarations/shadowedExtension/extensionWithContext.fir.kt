// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

interface Foo {
    fun foo(i: Int): Int

    context(c: String)
    fun bar(i: Int): Int
}

fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(i: Int) = i

context(c: String)
fun Foo.foo(i: Int) = i

context(c: String)
fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(i: Int) = i

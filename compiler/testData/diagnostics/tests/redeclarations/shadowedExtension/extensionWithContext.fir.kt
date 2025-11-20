// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

interface Foo {
    fun foo(i: Int): Int

    context(c: String)
    fun bar(i: Int): Int
}

fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(i: Int) = i

context(c: String)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(i: Int)<!> = i

context(c: String)
fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(i: Int) = i

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration */

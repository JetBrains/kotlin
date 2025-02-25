// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

interface Foo {
    fun foo(i: Int): Int

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(c: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun bar(i: Int): Int
}

<!CONFLICTING_OVERLOADS!>fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(i: Int)<!> = i

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(c: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(i: Int)<!> = i

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(c: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
fun Foo.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(i: Int) = i

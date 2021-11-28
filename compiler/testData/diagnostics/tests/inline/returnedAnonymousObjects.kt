// !LANGUAGE: -ApproximateAnonymousReturnTypesInPrivateInlineFunctions

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo1<!>(crossinline f: () -> Int) = object {
    fun bar(): Int = f()
}

interface I1
interface I2

private inline fun foo2(crossinline f: () -> Int) = object : I1 {
    fun bar(): Int = f()
}

private inline fun foo3(crossinline f: () -> Int) = object : I1, I2 {
    fun bar(): Int = f()
}

private fun foo4(f: () -> Int) = object {
    fun bar(): Int = f()
}

fun test1(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo1.<no name provided>")!>foo1 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo1.<no name provided>")!>foo1 { 2 }<!>
    }
    x.bar()
}

fun test2(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo2.<no name provided>")!>foo2 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo2.<no name provided>")!>foo2 { 2 }<!>
    }
    x.bar()
}

fun test3(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo3.<no name provided>")!>foo3 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo3.<no name provided>")!>foo3 { 2 }<!>
    }
    x.bar()
}

fun test4(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo4.<no name provided>")!>foo4 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo4.<no name provided>")!>foo4 { 2 }<!>
    }
    x.bar()
}

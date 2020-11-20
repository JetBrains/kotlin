private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo<!>(crossinline f: () -> Int) = object {
    fun bar(): Int = f()
}

fun test(b: Boolean) {
    var x = foo { 1 }
    if (b) {
        x = foo { 2 }
    }
    x.bar()
}
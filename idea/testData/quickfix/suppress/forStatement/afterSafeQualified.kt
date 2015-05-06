// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo(a: C) {
    @suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    a?.foo(""<caret>!!)
}

class C {
    fun foo(a: Any) {}
}
// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo(a: C) {
    a.foo(""<caret>!!)
}

class C {
    fun foo(a: Any) {}
}
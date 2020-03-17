class A {
    fun foo(s: String, flag: Boolean = true) {}
}

inline fun <T> T.myLet(block: (T) -> Unit) {}

fun test(a: A, s: String) {
    s.myLet(a::foo)
}
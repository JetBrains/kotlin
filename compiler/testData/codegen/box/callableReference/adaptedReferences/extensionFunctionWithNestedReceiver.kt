class A {
    class B
}

fun A.B.test(): Boolean {
    return true
}

fun foo(x: A.B.() -> Boolean): String { return "O"}
fun bar(x: (a: A.B) -> Boolean) : String { return "K" }

fun box(): String {
    return foo(A.B::test) + bar(A.B::test)
}
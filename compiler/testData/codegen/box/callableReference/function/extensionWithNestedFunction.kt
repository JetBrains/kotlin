class A(val a: String)

fun A.bar(a: A.() -> String): String {
    return a(this)
}

fun foo(block: A.(A.() -> String) -> String): String {
    return A("OK").block { a }
}

fun box(): String {
    return foo(A::bar)
}
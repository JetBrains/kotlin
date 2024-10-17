class A(val a: String)

fun A.foo(block: A.(A.() -> String) -> String): String {
    return A("OK").block { "" }
}

fun box(): String {
    return A("FAIL").foo {
        this.a
    }
}
class A(val a: String)

val x : A.(A.() -> String) -> String = { lambda: A.()-> String ->
    A("OK").lambda()
}

fun box(): String {
    return A("FAIL").x { this.a }
}
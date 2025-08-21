class A(val a: String)

val A.x : A.(A.() -> String) -> String
    get() = { a: A, lambda: A.()-> String ->
        this.lambda() + a.lambda() + A("").lambda()
    } as A.(A.() -> String) -> String

fun box(): String {
    return A("O").x(A("K"), { this.a } )
}

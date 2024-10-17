// IGNORE_BACKEND_K1: ANY
class A(val a: String)

val A.x : A.(A.() -> String) -> String
    get() = { a: A, lambda: A.()-> String ->
        this.lambda() + a.lambda() + A("").lambda()
    }

fun box(): String {
    return A("O").x(A("K"), { this.a } )
}
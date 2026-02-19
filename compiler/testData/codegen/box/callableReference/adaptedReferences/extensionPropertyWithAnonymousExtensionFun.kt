class A(val x: String)

val A.a : A.() -> String
    get() = fun A.(): String { return this@a.x + this.x}

fun foo(x: A.() -> String): String { return x(A("K")) }

fun box(): String {
    return foo(A::a.get(A("O")))
}
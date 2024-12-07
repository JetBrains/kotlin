class A(val x: String)

val <T> T.a : T.() -> String
    get() = fun1@ fun T.(): String { return (this@fun1 as A).x + (this@a as A).x }

fun box(): String {
    return A("K").a(A("O"))
}
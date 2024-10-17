class A(val x: String)

val <T> T.a : T.(String) -> String
    get() = fun T.(y: String): String { return (this@a as A).x + (this as A).x + y }

fun box(): String {
    return if (A("1").a(A("2"), "3") == "123") "OK" else "FAIL"
}
class A(val x: String)

val A.a : A.(String) -> String
    get() = fun A.(y: String): String { return this@a.x + this.x + y }

fun box(): String {
    return if (A("1").a(A("2"), "3") == "123") "OK" else "FAIL"
}
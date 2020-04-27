
class C(val x: String)

fun <T1 : C, T2 : T1> foo(x: T2): String =
    x.x

fun box(): String {
    return foo(C("OK"))
}
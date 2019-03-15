// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS, JVM_IR

fun foo(x: Int, vararg y: String = arrayOf("Aaa")): String =
        if (y[0].length == x) "OK" else "Fail"

fun use0(f: (Int) -> String): String = f(3)
fun use1(f: (Int, String) -> String): String = f(5, "Bbbbb")

fun box(): String {
    if (use0(::foo) != "OK") return "Fail"
    return use1(::foo)
}

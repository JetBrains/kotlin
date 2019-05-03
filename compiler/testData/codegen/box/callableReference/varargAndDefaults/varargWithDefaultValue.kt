// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS, JVM_IR

fun foo(x: Int, s: Int, vararg y: CharSequence = arrayOf("Aaa")): String =
        if (y.size == s && y[0].length == x) "OK" else "Fail"

fun use0(f: (Int, Int) -> String): String = f(3, 1)
fun use1(f: (Int, Int, String) -> String): String = f(5, 1, "Bbbbb")
fun use2(f: (Int, Int, String, String) -> String): String = f(5, 2, "Bbbbb", "Ccccc")

fun box(): String {
    if (use0(::foo) != "OK") return "Fail0"
    if (use1(::foo) != "OK") return "Fail1"
    return use2(::foo)
}

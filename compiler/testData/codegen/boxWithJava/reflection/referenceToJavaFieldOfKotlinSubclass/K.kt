open class K

fun box(): String {
    val f = J::value
    val a = J()
    return if (f.get(a) == 42) "OK" else "Fail: ${f[a]}"
}

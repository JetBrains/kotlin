fun a(): IntArray? = null

fun b() = throw Exception()

fun foo(): IntArray = a() ?: b()


fun box(): String {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}

fun a(): IntArray? = null

fun b(): Nothing = throw Exception()

fun foo(): IntArray = a() ?: b()


fun box(): String {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}

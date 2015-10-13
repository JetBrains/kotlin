fun a(): String? = null

fun b(): Nothing = throw Exception()

fun foo(): String = a() ?: b()


fun box(): String {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}

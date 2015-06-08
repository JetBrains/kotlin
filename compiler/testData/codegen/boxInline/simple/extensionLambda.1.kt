import test.*

fun box(): String {
    val p = "".test(50.0) {
        it
    }

    return if (p == 50.0) "OK" else "fail $p"
}
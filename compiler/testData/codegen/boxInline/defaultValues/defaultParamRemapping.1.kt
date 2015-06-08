import test.*

fun box(): String {
    val z = "OK".b { a, b ->
        a + b
    }

    return if (z == "s1OK") "OK" else "fail $z"
}
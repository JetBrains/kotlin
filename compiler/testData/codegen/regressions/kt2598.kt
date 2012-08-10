fun foo(condition: Boolean): String {
    val u = if (condition) {
        "OK"
    } else {
    }
    return u.toString()
}

fun box() = foo(true)

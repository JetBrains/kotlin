package test

inline fun <reified T> test(x: Any): Boolean {
    val x = object {
        val y = x is T
    }

    return x.y
}
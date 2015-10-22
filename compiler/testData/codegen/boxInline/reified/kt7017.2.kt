package test

inline fun test<reified T>(x: Any): Boolean {
    val x = object {
        val y = x is T
    }

    return x.y
}
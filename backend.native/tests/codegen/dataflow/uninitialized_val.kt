fun foo(b: Boolean): Int {
    val x: Int
    if (b) {
        x = 1
    } else {
        x = 2
    }

    return x
}

fun main(args: Array<String>) {
    val uninitializedUnused: Int

    println(foo(true))
    println(foo(false))
}
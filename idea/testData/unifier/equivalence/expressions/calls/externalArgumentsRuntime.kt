operator fun Int.contains(n: Int): Boolean = n < this

fun foo(n: Int): Int {
    if (n in 1) {
        println(1)
    }

    when(n) {
        <selection>in 1</selection> -> println(1)
        in 2 -> println(2)
    }

    if (1.contains(n)) {
        println(1)
    }

    when(n) {
        in 1 -> println(1)
    }

    return 1
}

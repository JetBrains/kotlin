package script

fun f(j: Int): Int {
    fun g(i: Int) = i * i *j

    return g(g(j))
}

val rv = f(2)

// expected: rv: 128
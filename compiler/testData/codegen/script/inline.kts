
// expected: rv: 2
inline fun foo(l: (Int) -> Int ): Int {
    return l(1)
}

fun bar(p: Int): Int {
    return p + 1
}

fun main(): Int {
    return foo { x -> bar(x) }
}

val rv = main()
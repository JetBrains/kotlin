// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: 2
fun foo(l: (Int) -> Int ): Int {
    return l(1)
}

fun bar(p: Int): Int {
    return p + 1
}

fun main(): Int {
    return foo { x -> bar(x) }
}

val rv = main()
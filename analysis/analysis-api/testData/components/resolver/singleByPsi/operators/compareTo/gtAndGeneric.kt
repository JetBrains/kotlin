class MyClass {
    operator fun compareTo(m: MyClass): Int = 0
}

fun usage(m: MyClass) {
    m <expr>></expr> 1
}

operator fun <T> T.compareTo(int: Int): Int = 0
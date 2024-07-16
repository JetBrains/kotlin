class MyClass {
    operator fun compareTo(m: MyClass): Int = 0
}

fun usage(m: MyClass) {
    m <expr>></expr> 1
}

operator fun <T> T.compareTo(int: Int): Int = 0
operator fun <R> R.compareTo(number: Int): Int = 1

// COMPILATION_ERRORS
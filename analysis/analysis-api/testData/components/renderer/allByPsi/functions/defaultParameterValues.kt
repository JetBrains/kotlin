fun primitives(
    int: Int = 42,
    negative: Int = -1,
    long: Long = 42L,
    boolean: Boolean = true,
    char: Char = 'c',
    string: String = "hello",
    double: Double = 3.14,
    float: Float = 2.5f,
) {}

fun nullDefault(value: String? = null) {}

fun computedConstant(value: Int = 40 + 2) {}

fun make(): String = ""

// A default value which is not a compile-time constant is rendered as a `...` placeholder.
fun nonConstant(value: String = make()) {}

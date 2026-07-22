// LANGUAGE: +StrictEquals

class Box<T>(val value: T) {
    override fun equals(@EqualityBound(Box::class) other: Any?): Boolean = value == other.value
}

fun box(): String {
    if (Box("a") != Box("a")) return "FAIL 1"
    if (Box("a") == Box("b")) return "FAIL 2"
    if (Box("a") == Box(1)) return "FAIL 3"
    if (Box<CharSequence?>("a") != Box<Any>("a")) return "FAIL 4"
    return "OK"
}

// LANGUAGE: +StrictEquals

class F(val tag: String) {
    operator fun invoke(): String = tag
    override fun equals(@EqualityBound(F::class) other: Any?): Boolean =
        this() == other()
}

fun box(): String {
    if (F("a") != F("a")) return "FAIL 1"
    if (F("a") == F("b")) return "FAIL 2"
    return "OK"
}

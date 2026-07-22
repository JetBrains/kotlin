// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

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

// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
class Carrier {
    companion {
        @PublishedApi
        internal var token = "O"

        @PublishedApi
        internal fun readToken(): String = token
    }
}

inline fun useCompanionBlockFromInlineDependency(): String {
    Carrier.token = "O"
    Carrier.token = Carrier.readToken() + "K"
    return Carrier.token
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val first = useCompanionBlockFromInlineDependency()
    if (first != "OK") return "FAIL: first call: $first"

    val second = useCompanionBlockFromInlineDependency()
    if (second != "OK") return "FAIL: second call: $second"

    return "OK"
}

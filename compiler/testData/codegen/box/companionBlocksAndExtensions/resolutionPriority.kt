// LANGUAGE: +CompanionBlocksAndExtensions

class Example {
    companion {
        fun test() = "block"
        val value = "blockValue"
    }

    companion object {
        fun test() = "object"
        val value = "objectValue"
    }
}

fun box(): String {
    // §2.1.1/§2.3.1: Companion block members shadow companion object members
    if (Example.test() != "block") return "FAIL: Example.test()=${Example.test()}"
    if (Example.value != "blockValue") return "FAIL: Example.value=${Example.value}"

    // Companion object members still accessible via Companion qualifier
    if (Example.Companion.test() != "object") return "FAIL: Companion.test()=${Example.Companion.test()}"
    if (Example.Companion.value != "objectValue") return "FAIL: Companion.value=${Example.Companion.value}"

    return "OK"
}

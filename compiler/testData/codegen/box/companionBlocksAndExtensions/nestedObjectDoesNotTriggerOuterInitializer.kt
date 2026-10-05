// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""

private fun init(tag: String, value: String): String {
    initLog += tag
    return value
}

class Outer {
    companion {
        val outerValue = init("O", "outer")
    }

    object Nested {
        val nestedValue = init("N", "nested")
        fun touch() = nestedValue
    }
}

fun box(): String {
    if (Outer.Nested.touch() != "nested") return "FAIL: nested"
    if (initLog != "N") return "FAIL: after nested: $initLog"

    if (Outer.outerValue != "outer") return "FAIL: outer"
    if (initLog != "NO") return "FAIL: after outer: $initLog"

    return "OK"
}

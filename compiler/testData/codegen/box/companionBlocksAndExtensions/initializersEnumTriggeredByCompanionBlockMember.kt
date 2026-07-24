// LANGUAGE: +CompanionBlocks +CompanionExtensions

var propertyInitLog = ""
var functionInitLog = ""

private fun initProperty(tag: String, value: String): String {
    propertyInitLog += tag
    return value
}

private fun initFunction(tag: String, value: String): String {
    functionInitLog += tag
    return value
}

enum class TriggeredByProperty(val value: String) {
    Entry(initProperty("1", "entry"));

    companion {
        val blockValue = initProperty("2", "block")
    }
}

enum class TriggeredByFunction(val value: String) {
    Entry(initFunction("A", "entry"));

    companion {
        val blockValue = initFunction("B", "block")
        fun touch() = "touch"
    }
}

fun box(): String {
    if (TriggeredByProperty.blockValue != "block") return "FAIL: property"
    if (TriggeredByProperty.Entry.value != "entry") return "FAIL: property entry"
    if (propertyInitLog != "12") return "FAIL: property order: $propertyInitLog"

    if (TriggeredByFunction.touch() != "touch") return "FAIL: function"
    if (TriggeredByFunction.Entry.value != "entry") return "FAIL: function entry"
    if (TriggeredByFunction.blockValue != "block") return "FAIL: function block"
    if (functionInitLog != "AB") return "FAIL: function order: $functionInitLog"

    return "OK"
}

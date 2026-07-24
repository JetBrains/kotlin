// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_BACKEND: JVM_IR
// JVM_IR initializes the second companion block before the companion
// object init block here (`ACBI` instead of `ABIC`)

var initLog = ""

private fun init(tag: String, value: String): String {
    initLog += tag
    return value
}

class Mixed {
    companion {
        val beforeObject = init("A", "before")
    }

    companion object {
        val objectValue = init("B", "object")

        init {
            initLog += "I"
        }
    }

    companion {
        val afterObject = init("C", "after")
    }
}

fun box(): String {
    if (Mixed.objectValue != "object") return "FAIL: object"
    if (initLog != "ABIC") return "FAIL: order after object: $initLog"

    if (Mixed.beforeObject != "before") return "FAIL: before"
    if (Mixed.afterObject != "after") return "FAIL: after"
    if (Mixed.Companion.objectValue != "object") return "FAIL: object access"
    if (initLog != "ABIC") return "FAIL: double init: $initLog"

    return "OK"
}

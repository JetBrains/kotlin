// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_BACKEND: JVM_IR
// JVM_IR KT-85853

var initLog = ""

interface I {
    companion {
        val blockA: String = run { initLog += "A"; "a" }
    }

    companion object {
        val objectB: String = run { initLog += "B"; "b" }

        init {
            initLog += "I"
        }
    }

    companion {
        val blockC: String = run { initLog += "C"; "c" }
    }
}

fun box(): String {
    if (I.blockA != "a") return "FAIL: blockA"
    if (I.objectB != "b") return "FAIL: objectB"
    if (I.blockC != "c") return "FAIL: blockC"
    if (I.Companion.objectB != "b") return "FAIL: Companion access"

    if (initLog != "ABIC") return "FAIL: order: $initLog"

    return "OK"
}

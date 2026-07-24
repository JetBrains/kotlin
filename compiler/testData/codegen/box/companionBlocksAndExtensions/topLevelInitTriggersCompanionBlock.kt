// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""

class Foo {
    companion {
        val bar: String = run { initLog += "F"; "bar" }
    }
}

val globalA: String = run { initLog += "A"; Foo.bar }
val globalB: String = run { initLog += "B"; Foo.bar }

fun box(): String {
    if (globalA != "bar") return "FAIL: globalA"
    if (globalB != "bar") return "FAIL: globalB"
    if (initLog != "AFB") return "FAIL: order: $initLog"

    return "OK"
}

// ISSUE: KT-91329
// LANGUAGE: +CompanionBlocks

var log = ""

private fun <T> log(x: T): T {
    log += x.toString() + ";"
    return x
}

private fun blackHole(x: Any) {}

interface I1 {
    companion {
        val staticProp: String = log("I1.staticProp")
    }

    // Needed for the static inititializer to be triggered, see §3.3 of the companion blocks KEEP
    fun unused() {}
}

open class A1 {
    companion {
        val staticProp: String = log("A1.staticProp")
    }
}

object EmptyObjectI1 : I1

object EmptyObjectA1 : A1()

interface I2 {
    companion {
        val staticProp: String = log("I2.staticProp")
    }

    // Needed for the static inititializer to be triggered, see §3.3 of the companion blocks KEEP
    fun unused() {}
}

open class A2 {
    companion {
        val staticProp: String = log("A2.staticProp")
    }
}

object EmptyObjectA2I2: I2, A2()

fun box(): String {
    if (log != "") return "FAIL 1: $log"

    blackHole(EmptyObjectI1)
    if (log != "I1.staticProp;") return "FAIL 2: $log"

    blackHole(EmptyObjectA1)
    if (log != "I1.staticProp;A1.staticProp;") return "FAIL 3: $log"

    blackHole(EmptyObjectA2I2)
    if (log != "I1.staticProp;A1.staticProp;A2.staticProp;I2.staticProp;") return "FAIL 4: $log"

    return "OK"
}

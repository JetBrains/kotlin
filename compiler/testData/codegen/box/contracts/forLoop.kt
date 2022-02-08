// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

fun test1(): String {
    var res = ""
    for (s in listOf("OK")) {
        runOnce {
            res += s
        }
    }
    return res
}

fun test2(): String {
    var res = ""
    for (s: String in listOf("OK")) {
        runOnce {
            res += s
        }
    }
    return res
}

fun test3(): String {
    var res = ""
    for ((s, _) in listOf("OK" to "FAIL")) {
        runOnce {
            res += s
        }
    }
    return res
}

fun test4(): String {
    var res = ""
    for ((s: String, _) in listOf("OK" to "FAIL")) {
        runOnce {
            res += s
        }
    }
    return res
}

fun box(): String {
    test1().let { if (it != "OK") return "FAIL 1: $it" }
    test2().let { if (it != "OK") return "FAIL 2: $it" }
    test3().let { if (it != "OK") return "FAIL 3: $it" }
    test4().let { if (it != "OK") return "FAIL 4: $it" }
    return "OK"
}

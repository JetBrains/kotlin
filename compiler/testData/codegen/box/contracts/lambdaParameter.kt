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

fun o(): String {
    var res = "FAIL1 "
    ("O" to "").let { (a, _) ->
        runOnce {
            res = a
        }
    }
    return res
}

fun k(): String {
    val res: String
    "K".let { b ->
        runOnce {
            res = b
        }
    }
    return res
}

fun box(): String {
    return o() + k()
}

// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// JVM_ABI_K1_K2_DIFF: KT-62464

import kotlin.contracts.*

class Smth {
    val whatever: Int

    init {
        calculate { whatever = it }
    }

    context(Any)
    inline fun calculate(block: (Int) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
    }
}

fun box(): String {
    val s = Smth()
    return "OK"
}
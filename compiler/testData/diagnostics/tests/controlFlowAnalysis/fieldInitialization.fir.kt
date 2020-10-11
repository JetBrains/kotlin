// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.internal.ContractsDsl

import kotlin.contracts.*

@kotlin.contracts.ExperimentalContracts
inline fun inlineMe(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
inline fun crossinlineMe(crossinline block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@Suppress("NOTHING_TO_INLINE")
@kotlin.contracts.ExperimentalContracts
inline fun noinlineMe(noinline block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
fun notinline(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
class Test {
    val a: String
    val b: String
    val c: String
    val d: String

    init {
        inlineMe {
            a = "allowed"
        }
        crossinlineMe {
            b = "not allowed"
        }
        noinlineMe {
            c = "not allowed"
        }
        notinline {
            d = "not allowed"
        }
    }
}

@kotlin.contracts.ExperimentalContracts
class Test1 {
    val a: String = ""
    val b: String = ""
    val c: String = ""
    val d: String = ""

    init {
        inlineMe {
            <!VARIABLE_EXPECTED!>a<!> += "allowed"
        }
        crossinlineMe {
            <!VARIABLE_EXPECTED!>b<!> += "not allowed"
        }
        noinlineMe {
            <!VARIABLE_EXPECTED!>c<!> += "not allowed"
        }
        notinline {
            <!VARIABLE_EXPECTED!>d<!> += "not allowed"
        }
    }
}

@kotlin.contracts.ExperimentalContracts
class Test2 {
    val a: String = ""
    val b: String = ""
    val c: String = ""
    val d: String = ""

    init {
        var blackhole = ""
        inlineMe {
            blackhole += a
        }
        crossinlineMe {
            blackhole += b
        }
        noinlineMe {
            blackhole += c
        }
        notinline {
            blackhole += d
        }
    }
}

@kotlin.contracts.ExperimentalContracts
class Test4 {
    val a: String = ""
    val b: String = ""
    val c: String = ""
    val d: String = ""

    init {
        var blackhole: String
        inlineMe {
            blackhole = a
        }
        crossinlineMe {
            blackhole = b
        }
        noinlineMe {
            blackhole = c
        }
        notinline {
            blackhole = d
        }
    }
}

@kotlin.contracts.ExperimentalContracts
class Test5 {
    val a: String
    val b: String
    val c: String
    val d: String

    val aInit = inlineMe {
        a = "OK"
    }
    val bInit = crossinlineMe {
        b = "OK"
    }
    val cInit = noinlineMe {
        c = "OK"
    }
    val dInit = notinline {
        d = "OK"
    }
}

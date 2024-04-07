// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !OPT_IN: kotlin.internal.ContractsDsl

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
    val e: String
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val f: String<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val g: String<!>
    val h: String
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val i: String<!>

    init {
        inlineMe {
            a = "allowed"
        }
        crossinlineMe {
            <!CAPTURED_VAL_INITIALIZATION!>b<!> = "not allowed"
        }
        noinlineMe {
            <!CAPTURED_VAL_INITIALIZATION!>c<!> = "not allowed"
        }
        notinline {
            <!CAPTURED_VAL_INITIALIZATION!>d<!> = "not allowed"
        }

        crossinlineMe {
            inlineMe {
                e = "not allowed"
            }
        }

        fun localFun() {
            <!CAPTURED_MEMBER_VAL_INITIALIZATION!>f<!> = "not allowed"
        }

        val <!UNUSED_VARIABLE!>localLambda<!> = {
            <!CAPTURED_MEMBER_VAL_INITIALIZATION!>g<!> = "not allowed"
        }

        object {
            val o: String

            init {
                h = "not allowed"
                o = "allowed"
            }
        }

        class Local {
            init {
                <!CAPTURED_MEMBER_VAL_INITIALIZATION!>i<!> = "not allowed"
            }
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
            <!VAL_REASSIGNMENT!>a<!> += "allowed"
        }
        crossinlineMe {
            <!VAL_REASSIGNMENT!>b<!> += "not allowed"
        }
        noinlineMe {
            <!VAL_REASSIGNMENT!>c<!> += "not allowed"
        }
        notinline {
            <!VAL_REASSIGNMENT!>d<!> += "not allowed"
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
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>blackhole<!>: String
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
        <!CAPTURED_VAL_INITIALIZATION!>b<!> = "not allowed"
    }
    val cInit = noinlineMe {
        <!CAPTURED_VAL_INITIALIZATION!>c<!> = "not allowed"
    }
    val dInit = notinline {
        <!CAPTURED_VAL_INITIALIZATION!>d<!> = "not allowed"
    }
}

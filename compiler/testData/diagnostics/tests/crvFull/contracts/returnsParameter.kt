// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T> id(x: T): T {
    contract {
        returnsParameter(x)
    }
    return x
}

inline fun <T> T.myAlso(block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myAlso)
    }
    block(this)
    return this
}

fun <T> choose(cond: Boolean, a: T, b: T): T {
    contract {
        returnsParameter(a)
        returnsParameter(b)
    }
    return if (cond) a else b
}

fun fooS(): String = ""

fun fooSN(): String? = null

@IgnorableReturnValue
fun ign(): String = ""

fun unit(): Unit = Unit

fun testValueParameter(s: String) {
    // Argument is a non-ignorable call => reported
    id(<!RETURN_VALUE_NOT_USED!>fooS<!>())

    // Argument is an ignorable call => not reported
    id(ign())
    id(unit())

    // Argument is a plain variable read => not reported
    id(s)

    // Result is actually used => not reported
    val x = id(fooS())
    println(x)
}

fun testReceiver(s: String) {
    // Receiver is a non-ignorable call => reported
    <!RETURN_VALUE_NOT_USED!>fooS<!>().myAlso { }
}

fun testReceiverIgnorable(s: String) {
    // Receiver is ignorable / a plain variable => not reported
    ign().myAlso { }
    s.myAlso { }
}

fun testChain() {
    <!RETURN_VALUE_NOT_USED!>fooS<!>().myAlso { }.myAlso { }
}

fun testMultipleParams(c: Boolean) {
    choose(c, <!RETURN_VALUE_NOT_USED!>fooS<!>(), <!RETURN_VALUE_NOT_USED!>fooS<!>())
    choose(c, ign(), ign())
    choose(c, ign(), <!RETURN_VALUE_NOT_USED!>fooS<!>())
    choose(c, <!RETURN_VALUE_NOT_USED!>fooS<!>(), ign())
}

fun testSafeCall() {
    // Ignorability is propagated through the safe-call receiver as well.
    <!RETURN_VALUE_NOT_USED!>fooSN<!>()?.myAlso { }
}

class Holder {
    inline fun applySelf(block: () -> Unit): Holder {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            returnsParameter(this@Holder)
        }
        block()
        return this
    }

    fun holderProducer(): Holder = Holder()

    fun testExplicitReceiver(anotherHolder: Holder) {
        applySelf { }
        this.applySelf { }
        anotherHolder.applySelf { }
        <!RETURN_VALUE_NOT_USED!>holderProducer<!>().applySelf { }
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, stringLiteral,
thisExpression, typeParameter */

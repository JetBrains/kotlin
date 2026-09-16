// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T : Any, R> T?.myLetNotNull(block: (T) -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returnsNotNull() implies (this@myLetNotNull != null)
        returnsResultOf(block)
    }
    return if (this != null) block(this) else null
}

inline fun <T, R> T.myLetWithReturnsTrue(predicate: (T) -> Boolean, block: (T) -> R): R? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returnsResultOf(block)
    }
    return if (predicate(this)) block(this) else null
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testReturnsNotNullWithIgnorable(s: String?) {
    s.myLetNotNull { ign() }
}

fun testReturnsNotNullWithNonIgnorable(s: String?) {
    s.<!RETURN_VALUE_NOT_USED!>myLetNotNull<!> { fooS() }
}

fun testReturnsTrueWithIgnorable(s: String) {
    s.myLetWithReturnsTrue({ it.isNotEmpty() }, { ign() })
}

fun testReturnsTrueWithNonIgnorable(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLetWithReturnsTrue<!>({ it.isNotEmpty() }, { fooS() })
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractConditionalEffect, contracts, funWithExtensionReceiver,
functionDeclaration, functionalType, ifExpression, inline, lambdaLiteral, nullableType, thisExpression, typeParameter */

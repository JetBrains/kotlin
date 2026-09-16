// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

inline fun <T, R> T.myRun(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block()
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testNestedBothIgnorable(s: String) {
    s.myLet { it.myRun { ign() } }
}

fun testNestedBothNonIgnorable(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { it.myRun { fooS() } }
}

fun testNestedOuterIgnorableInnerNot(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        it.myRun { fooS() }
    }
}

fun testNestedOuterNotIgnorableInnerIs(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> {
        val x = it.myRun { ign() }
        fooS()
    }
}

fun testDeeplyNested(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!> { a ->
        a.myRun {
            this.myLet { b ->
                b.myRun {
                    fooS()
                }
            }
        }
    }
}

fun testDeeplyNestedIgnorable(s: String) {
    s.myLet { a ->
        a.myRun {
            this.myLet { b ->
                b.myRun {
                    ign()
                }
            }
        }
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, thisExpression, typeParameter */

// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T, R> T.myLetCrossinline(crossinline block: (T) -> R): R {
    contract {
        returnsResultOf(block)
    }
    return block(this)
}

<!NOTHING_TO_INLINE!>inline<!> fun <T, R> T.myLetNoinline(noinline block: (T) -> R): R {
    contract {
        returnsResultOf(block)
    }
    return block(this)
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testCrossinlineIgnorable(s: String) {
    s.myLetCrossinline { ign() }
}

fun testCrossinlineNonIgnorable(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLetCrossinline<!> { fooS() }
}

fun testNoinlineIgnorable(s: String) {
    s.myLetNoinline { ign() }
}

fun testNoinlineNonIgnorable(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLetNoinline<!> { fooS() }
}

fun testCrossinlineNonIgnorableResult(s: String): String {
    s.<!RETURN_VALUE_NOT_USED!>myLetCrossinline<!> {
        fooS()
    }
    return ""
}

/* GENERATED_FIR_TAGS: contracts, funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
nullableType, thisExpression, typeParameter */

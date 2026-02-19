// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUES: KT-80250, KT-80900, KT-81680

// should be commented out to trigger error conditions descibed in the issues
// import kotlin.contracts.*

// from KT-80250
fun myBuilder(build: () -> Unit) {
    <!UNRESOLVED_REFERENCE!>contract<!> {
        <!UNRESOLVED_REFERENCE!>callsInPlace<!>(build, <!UNRESOLVED_REFERENCE!>InvocationKind<!>.EXACTLY_ONCE)
    }
}

// from KT-80900
fun <T> T.alsoIf(condition: Boolean, block: (T) -> Unit): T {
    <!UNRESOLVED_REFERENCE!>contract<!> {
        // Declares that the lambda runs at most once
        <!UNRESOLVED_REFERENCE!>callsInPlace<!>(block, <!UNRESOLVED_REFERENCE!>InvocationKind<!>.AT_MOST_ONCE)
        // Declares that the condition is assumed to be true inside the lambda
        condition <!UNRESOLVED_REFERENCE!>holdsIn<!> block
    }
    if (condition) block(this)
    return this
}

fun useApplyIf(input: Any) {
    val result = listOf(1, 2, 3)
        .first()
        .alsoIf(input is Int) {
            // The input parameter is smart cast to Int inside the lambda
            // Prints the sum of input and first list element
            println(input <!UNRESOLVED_REFERENCE!>+<!> it)
            // 2
        }
        .toString()
}

// from KT-81680
private fun validate(request: String?) {
    <!UNRESOLVED_REFERENCE!>contract<!> {
        <!UNRESOLVED_REFERENCE!>returns<!>() implies (request != null)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, classReference, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, ifExpression, integerLiteral, isExpression, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, thisExpression, typeParameter */

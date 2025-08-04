// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79672
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

sealed interface Sealed {
    sealed interface A : Sealed {
        interface A1 : A
        interface A2 : A
    }
    interface B : Sealed
}

fun test(it: Sealed): Int {
    if (true) {
        if (it is Sealed.A) return 0
    } else {
        if (it is Sealed.A) return 1
        if (it is Sealed.A.A1) return 2
    }
    // From the first branch we get `it: ¬A`, and from the second one, we get `it: ¬(A | A1)`.
    // We then merge the two statements, and should theoretically get `it: ¬A`.

    return <!NO_ELSE_IN_WHEN!>when<!> (it) {
        is Sealed.B -> 3
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nestedClass, sealed,
smartcast, whenExpression, whenWithSubject */

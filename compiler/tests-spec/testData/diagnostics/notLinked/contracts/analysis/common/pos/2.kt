// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, common
 NUMBER: 2
 DESCRIPTION: Recursion in the lambda of contract function.
 */

fun case_1(x: Int): Unit = funWithExactlyOnceCallsInPlace {
    if (x == 0) return
    if (x == 1) return
    return case_1(x - 2)
}

fun case_2(x: Int) {
    funWithAtLeastOnceCallsInPlace {
        if (x == 0) return
        if (x == 1) return
        return case_2(x - 2)
    }
}

fun case_3(x: Int) {
    funWithAtMostOnceCallsInPlace {
        if (x == 0) return
        if (x == 1) return
        return case_3(x - 2)
    }
}

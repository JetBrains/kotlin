// ISSUE: KT-66512

typealias MyUnit = Unit

// Note that resolution works differently for lambdas passed as function arguments and lambdas assigned to variables,
// thus we need to test both cases.

// ================= Lambdas assigned to a variable =================

val expectedMyUnitExplicitReturnString: () -> MyUnit = l@ {
    return@l <!RETURN_TYPE_MISMATCH!>""<!>
}

// ============== Lambdas passed as function argument ===============

fun test() {
    run<MyUnit> l@ {
        return@l <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>""<!>
    }
}

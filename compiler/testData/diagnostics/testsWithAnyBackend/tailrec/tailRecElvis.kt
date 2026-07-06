// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNREACHABLE_CODE

// Elvis on the RHS: the last operand of elvis is a tail position
tailrec fun elvisRhs(x: Int): Int {
    return maybe(x) ?: elvisRhs(x - 1)
}

// Elvis on the LHS: the call is NOT in tail position because elvis may evaluate the RHS after it
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun elvisLhs(x: Int): Int? {
    return <!NON_TAIL_RECURSIVE_CALL!>elvisLhs<!>(x - 1) ?: 0
}<!>

// Nested elvis: only the very last operand is in tail position
tailrec fun elvisNested(x: Int): Int {
    return maybe(x) ?: maybe(x - 1) ?: elvisNested(x - 2)
}

// Elvis where the tail call is in the middle (not tail)
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun elvisMiddle(x: Int): Int? {
    return maybe(x) ?: <!NON_TAIL_RECURSIVE_CALL!>elvisMiddle<!>(x - 1) ?: 0
}<!>

fun maybe(x: Int): Int? = x.takeIf { it > 0 }

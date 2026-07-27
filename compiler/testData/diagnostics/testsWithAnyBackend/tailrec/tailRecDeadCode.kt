// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNREACHABLE_CODE

// Tail call after unconditional return — the recursive call is dead code.
// FIR does not count dead-code calls, so NO_TAIL_CALLS_FOUND is reported.
// IR collector does not check for dead code, so it still finds the call — no NO_TAIL_CALLS_FOUND_IN_IR.
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadAfterReturn(x: Int): Int {
    return 1
    return deadAfterReturn(x - 1)
}

// Tail call after throw — also dead code
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadAfterThrow(x: Int): Int {
    throw RuntimeException()
    return deadAfterThrow(x - 1)
}

// Non-tail call in dead code — no tail calls at all
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadNonTail(x: Int): Int {
    return 1
    deadNonTail(x - 1)
    return 2
}<!>

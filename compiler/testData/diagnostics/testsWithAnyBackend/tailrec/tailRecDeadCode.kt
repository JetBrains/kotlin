// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNREACHABLE_CODE

// Tail call after unconditional return — the recursive call is dead code and does not count as a tail call.
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadAfterReturn(x: Int): Int {
    return 1
    return deadAfterReturn(x - 1)
}

// Tail call after throw — also dead code and not a tail call
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadAfterThrow(x: Int): Int {
    throw RuntimeException()
    return deadAfterThrow(x - 1)
}

// Non-tail call in dead code — the call is ignored, leaving no tail calls
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun deadNonTail(x: Int): Int {
    return 1
    deadNonTail(x - 1)
    return 2
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral, tailrec */

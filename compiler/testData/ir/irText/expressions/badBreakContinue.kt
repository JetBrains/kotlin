// IGNORE_ERRORS
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -BREAK_OR_CONTINUE_OUTSIDE_A_LOOP -NOT_A_LOOP_LABEL -UNRESOLVED_REFERENCE -BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND_K1: JS_IR
// RUN_PIPELINE_TILL: FIR2IR

fun test1() {
    break
    continue
}

fun test2() {
    L1@ while (true) {
        break@ERROR
        continue@ERROR
    }
}

fun test3() {
    L1@ while (true) {
        val lambda = {
            break@L1
            continue@L1
        }
    }
}

fun test4() {
    while (break) {}
    while (continue) {}
}

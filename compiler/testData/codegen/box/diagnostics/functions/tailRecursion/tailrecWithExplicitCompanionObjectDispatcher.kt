// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JVM
// IGNORE_FIR_DIAGNOSTICS_DIFF

class C {
    companion object {
        <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun rec(i: Int) {
            if (i <= 0) return
            C.<!NON_TAIL_RECURSIVE_CALL!>rec<!>(i - 1)
        }
    }
}

fun box(): String {
    C.rec(100000)
    return "OK"
}

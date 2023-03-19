// !LANGUAGE: +SuspendConversion

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57244 K2: slightly different naming scheme for suspend conversion adapters

fun myApply(f: suspend () -> Unit) {}

fun test(f: () -> Unit) {
    myApply(f)
}

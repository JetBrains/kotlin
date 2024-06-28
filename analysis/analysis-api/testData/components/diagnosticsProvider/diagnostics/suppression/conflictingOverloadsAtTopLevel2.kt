// IGNORE_FIR
// In FIR, there is currently a mismatch between the file's diagnostics and the individual elements' diagnostics, so we have to disable the
// FIR test. It will be fixed by KT-63221.

class SomeClass

fun someFun(): Int {
    return 5
}

@Suppress("CONFLICTING_OVERLOADS")
fun someFun(): SomeClass {
    return SomeClass()
}

@Suppress("CONFLICTING_OVERLOADS")
fun someFun() {
}

fun someFun(): String {
    return ""
}

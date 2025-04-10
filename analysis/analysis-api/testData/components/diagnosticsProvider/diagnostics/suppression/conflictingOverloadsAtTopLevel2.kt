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

// SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK: KT-63221
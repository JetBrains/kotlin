// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57428

fun use(fn: () -> Unit) {}

fun test() {
    use { 42 }
}

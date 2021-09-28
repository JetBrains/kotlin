// FIR_IDENTICAL

fun use(fn: () -> Unit) {}

fun test() {
    use { 42 }
}

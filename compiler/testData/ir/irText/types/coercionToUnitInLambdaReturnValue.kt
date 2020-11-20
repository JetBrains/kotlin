// FIR_IDENTICAL
// !LANGUAGE: +NewInference

fun use(fn: () -> Unit) {}

fun test() {
    use { 42 }
}
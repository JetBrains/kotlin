// !LANGUAGE: +NewInference

fun use(fn: () -> Unit) {}

fun test() {
    use { 42 }
}
// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ContextReceivers

fun <T> test(action: context(T) () -> Unit) {}

fun <T> test2(actionWithArg: context(T) (T) -> Unit) {}

fun main() {
    test<String> {
        length
    }
    test<Int> {
        toDouble()
    }
    test2<String> { a ->
        length
        a.length
    }
}

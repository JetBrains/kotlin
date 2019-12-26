// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNCHECKED_CAST -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// ISSUE: KT-35702

interface A

fun <T> uncheckedCast(value: Any?): T = value as T

fun <K> select(x: K, y: K): K = x

fun <T : A> test_1(b: Boolean, s: String) {
    val result: (A) -> T = if (b) {
        { a: A -> uncheckedCast(s) }
    } else {
        { a: A -> uncheckedCast(s) }
    }
}

fun <T : A> test_2(b: Boolean, s: String) {
    val result: (A) -> T = if (b) {
        { a: A -> uncheckedCast(s) }
    } else {
        { a -> uncheckedCast(s) }
    }
}

fun <T : A> test_3(b: Boolean, s: String) {
    val result: (A) -> T = if (b) {
        { a -> uncheckedCast(s) }
    } else {
        { a -> uncheckedCast(s) }
    }
}

fun <T : A> test_4(s: String) {
    val result: (A) -> T = select(
        { a: A -> uncheckedCast(s) },
        { a: A -> uncheckedCast(s) }
    )
}

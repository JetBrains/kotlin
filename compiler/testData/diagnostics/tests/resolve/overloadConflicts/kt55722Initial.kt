// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// SKIP_TXT

interface MyConsumer<T> {
    fun consume(x: T) {}
}

fun foo(x: MyConsumer<in CharSequence>?, v: CharSequence) {
    if (x != null) {
        x.consume(v)
    }
}

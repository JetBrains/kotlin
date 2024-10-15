// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun test(c: C<out Number>, list: MutableList<out Number>, consumer: Consumer<*>) {
    list.add(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)
    list.bar(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)

    with(c) {
        list.foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)
    }

    consumer.consume(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)
}

class C<T> {
    fun <T> MutableList<T>.foo(t: T){}
}

fun <T> MutableList<T>.bar(t: T){}

fun <T : MutableList<out Number>> test2(t: T) {
    t.add(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)
}

interface Consumer<T : Number> {
    fun consume(t: T)
}
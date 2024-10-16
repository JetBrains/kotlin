// RENDER_DIAGNOSTICS_FULL_TEXT

fun test(c: C<out Number>, list: MutableList<out Number>, consumer: Consumer<*>) {
    list.add(<!MEMBER_PROJECTED_OUT!>42<!>)
    list.bar(<!MEMBER_PROJECTED_OUT!>42<!>)

    with(c) {
        list.foo(<!MEMBER_PROJECTED_OUT!>42<!>)
    }

    consumer.consume(<!MEMBER_PROJECTED_OUT!>42<!>)
}

class C<T> {
    fun <T> MutableList<T>.foo(t: T){}
}

fun <T> MutableList<T>.bar(t: T){}

fun <T : MutableList<out Number>> test2(t: T) {
    t.add(<!MEMBER_PROJECTED_OUT!>42<!>)
}

interface Consumer<T : Number> {
    fun consume(t: T)
}
// ISSUE: KT-65712
// RENDER_DIAGNOSTICS_FULL_TEXT

fun test(a: BodySpec<List<*>, *>) {
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<List<*>, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<*, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<Int, *><!>>()
}

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> value(): T
}

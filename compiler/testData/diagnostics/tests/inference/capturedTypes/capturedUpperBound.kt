// ISSUE: KT-65712

fun test(a: BodySpec<List<*>, *>) {
    a.value<BodySpec<List<*>, *>>()
    a.value<BodySpec<*, *>>()
    a.value<BodySpec<Int, *>>()
}

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> value(): T
}

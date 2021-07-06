// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B): T
}

fun test(b: BodySpec<String, *>) {
    val x = b.isEqualTo("")
    <!DEBUG_INFO_EXPRESSION_TYPE("BodySpec<*, *>")!>x<!>
}

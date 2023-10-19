// !LANGUAGE: -TypeInferenceOnCallsWithSelfTypes

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B): T
}

fun test(b: BodySpec<String, *>) {
    val x = b.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>isEqualTo<!>("")
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for b.isEqualTo(\"\")]")!>x<!>
}

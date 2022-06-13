// !LANGUAGE: -TypeInferenceOnCallsWithSelfTypes

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B): T
}

fun test(b: BodySpec<String, *>) {
    val x = b.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>isEqualTo<!>("")
    x
}
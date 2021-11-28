// !LANGUAGE: -TypeInferenceOnCallsWithSelfTypes

class Builder<B : Builder<B>> {
    fun <T : B> test(): T = TODO()
    fun foo() {}
}

fun testStar(builder: Builder<*>) {
    builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!UNRESOLVED_REFERENCE!>foo<!>()
}

fun <K : Builder<K>> testTypeParam(builder: Builder<K>) {
    builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!UNRESOLVED_REFERENCE!>foo<!>()
}
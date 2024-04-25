// LANGUAGE: -TypeInferenceOnCallsWithSelfTypes

class Builder<B : Builder<B>> {
    fun <T : B> test(): T = TODO()
    fun foo() {}
}

fun testStar(builder: Builder<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()<!>

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()
}

fun <K : Builder<K>> testTypeParam(builder: Builder<K>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>builder.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()<!>

    builder
        .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!>()
        .<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()
}

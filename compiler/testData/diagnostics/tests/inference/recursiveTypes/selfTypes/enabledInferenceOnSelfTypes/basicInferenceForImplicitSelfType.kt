// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

class Builder<B : Builder<B>> {
    fun <T : B> test(): T = TODO()

    fun foo() {}
}

fun testStar(builder: Builder<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Builder<*>")!>builder.test()<!>

    builder
        .test()
        .foo()
}

fun <K : Builder<K>> testTypeParam(builder: Builder<K>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("K")!>builder.test()<!>

    builder
        .test()
        .foo()
}

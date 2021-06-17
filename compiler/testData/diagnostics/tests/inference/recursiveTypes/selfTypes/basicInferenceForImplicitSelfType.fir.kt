class Builder<B : Builder<B>> {
    fun <T : B> test(): T = TODO()

    fun foo() {}
}

fun testStar(builder: Builder<*>) {
    builder.test()

    builder
        .test()
        .foo()
}

fun <K : Builder<K>> testTypeParam(builder: Builder<K>) {
    builder.test()

    builder
        .test()
        .foo()
}

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B): T
}

fun test(b: BodySpec<String, *>) {
    b.isEqualTo("")
}
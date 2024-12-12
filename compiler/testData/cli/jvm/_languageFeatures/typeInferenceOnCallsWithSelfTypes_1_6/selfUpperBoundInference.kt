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

    WriterAppender.newBuilder()
    WriterAppender.Builder1()
    WriterAppender.intersectTwoSelfTypes()
}

object WriterAppender {
    interface Builder2<K : Builder2<K>>

    class Builder1<B : Builder1<B>> {
        fun asBuilder(): B {
            return this as B
        }
    }

    fun <B : Builder1<B>> newBuilder(): B {
        return Builder1<B>().asBuilder()
    }

    fun <B> intersectTwoSelfTypes(): B where B : Builder1<B>, B: Builder2<B> {
        return Builder1<B>().asBuilder()
    }
}

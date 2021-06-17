fun test() {
    WriterAppender.newBuilder()
}

object WriterAppender {
    class Builder<B : Builder<B>> {
        fun asBuilder(): B {
            return this as B
        }
    }

    fun <B : Builder<B>> newBuilder(): B {
        return Builder<B>().asBuilder()
    }
}
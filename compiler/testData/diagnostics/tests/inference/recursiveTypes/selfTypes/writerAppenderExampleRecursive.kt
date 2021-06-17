fun test() {
    WriterAppender.<!TYPE_MISMATCH!>newBuilder<!>()
}

object WriterAppender {
    class Builder<B : Builder<B>> {
        fun asBuilder(): B {
            return this <!UNCHECKED_CAST!>as B<!>
        }
    }

    fun <B : Builder<B>> newBuilder(): B {
        return Builder<B>().asBuilder()
    }
}
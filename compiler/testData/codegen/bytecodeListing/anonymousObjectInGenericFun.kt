// WITH_SIGNATURES

fun <T> test() {
    val x = object {
        fun <S1> foo() {}

        fun <S2> S2.ext() {}

        val <S3> S3.extVal
            get() = 1

        var <S4> S4.extVar
            get() = 1
            set(value) {}
    }

    x.foo<Any>()
}

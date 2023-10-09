// JVM_ABI_K1_K2_DIFF: KT-62714

interface IFoo {
    fun foo(e: En): String
}

enum class En {
    TEST {
        inner class Nested : IFoo {
            private val ee = TEST

            override fun foo(e: En): String {
                return if (e == ee) e.ok else "Failed"
            }
        }

        override val ok: String get() = "OK"
        override fun foo(): IFoo = Nested()
    },
    OTHER {
        override val ok: String get() = throw AssertionError()
        override fun foo(): IFoo = throw AssertionError()
    };

    abstract val ok: String
    abstract fun foo(): IFoo
}

fun box() = En.TEST.foo().foo(En.TEST)

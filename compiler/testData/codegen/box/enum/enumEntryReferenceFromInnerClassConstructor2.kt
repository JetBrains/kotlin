// JVM_ABI_K1_K2_DIFF: KT-62714, KT-63880

interface IFoo {
    fun foo(): String
}

interface IBar {
    fun bar(): String
}

enum class Test : IFoo, IBar {
    FOO {
        // FOO referenced from inner class constructor with initialized 'this'
        inner class Inner {
            val fooFoo = FOO.foo()
        }

        val z = Inner()

        override fun foo() = "OK"

        override fun bar() = z.fooFoo
    }
}

fun box() = Test.FOO.bar()
package test

class ClassWithClassObject {
    fun f() {
    }

    companion object {
        fun f() {
        }

        fun Int.f() {
        }

        private fun privateFun() {
        }

        val a: A = A()

        public var b: B = B()

        val Int.g: Int
            get() = this + 2

        fun <T, K, G> complexFun(a: T, b: K, c: G): G {
            throw RuntimeException()
        }
    }

    class B {
    }

    class A {
    }
}

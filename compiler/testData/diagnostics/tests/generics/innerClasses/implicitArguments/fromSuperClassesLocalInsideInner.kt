// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER

class A<R1, R2, R3, R4, R5, R6>

class Outer<T> {
    inner class Inner<F> {
        private fun <E> foobar() = {
            open class LocalOuter<X, Y> {
                inner class LocalInner<Z> {
                    fun a() = A<T, F, E, X, Y, Z>()
                }
            }

            class Derived : LocalOuter<Double, Short>() {
                fun foo(): LocalInner<Long> = null!!
            }

            Derived()
        }

        private fun noParameters() = {
            open class LocalOuter2<X, Y> {
                inner class LocalInner2<Z> {
                    fun a() = A<T, F, Any, X, Y, Z>()
                }
            }

            class Derived2 : LocalOuter2<Double, Short>() {
                fun foo(): LocalInner2<Long> = null!!
            }
            Derived2()
        }

        fun test(z: Outer<String>.Inner<F>) {
            var x = foobar<String>()
            x = foobar<String>()

            x().foo().a() checkType { _<A<T, F, String, Double, Short, Long>>() }

            x = <!TYPE_MISMATCH!>foobar<Int>()<!>
            x = <!TYPE_MISMATCH!>z.foobar<String>()<!>

            var y = noParameters()
            y = noParameters()

            y().foo().a() checkType { _<A<T, F, Any, Double, Short, Long>>() }
        }
    }
}

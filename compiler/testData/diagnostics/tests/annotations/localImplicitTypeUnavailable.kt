// FIR_IDENTICAL
package second

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: KClass<*>)

fun check() {
    class A {
        val bar get() = B().foo
        fun baz() = B().doo()

        private inner class B {
            var foo: @Anno(C::class) List<@Anno(C::class) Collection<@Anno(C::class) String>>? = null
            fun doo(): @Anno(C::class) List<@Anno(C::class) Collection<@Anno(C::class) String>>? = null
            private inner class C
        }
    }

    val a = A().bar
    val b = A().baz()
}

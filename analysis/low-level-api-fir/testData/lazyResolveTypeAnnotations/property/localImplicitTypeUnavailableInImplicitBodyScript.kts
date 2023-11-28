package second

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: KClass<*>)
fun <T> lambda(action: () -> T): T = action()

fun c<caret>heck() = lambda {
    class A {
        val bar get() = B().foo

        private inner class B {
            var foo: @Anno(C::class) List<@Anno(C::class) Collection<@Anno(C::class) String>>? = null
            private inner class C
        }
    }

    A().bar
}

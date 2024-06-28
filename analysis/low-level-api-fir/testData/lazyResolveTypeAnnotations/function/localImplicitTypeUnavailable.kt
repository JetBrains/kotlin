// BODY_RESOLVE
package second

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: KClass<*>)

fun c<caret>heck() {
    class A {
        fun bar() = B().foo()

        private inner class B {
            fun foo(): @Anno(C::class) List<@Anno(C::class) Collection<@Anno(C::class) String>>? = null
            private inner class C
        }
    }

    val a = A().bar()
}

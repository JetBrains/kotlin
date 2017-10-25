// ALLOW_AST_ACCESS

package test

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Ann(val klass: KClass<*>)

class A {
    fun simple(s: @Ann(Simple::class) String) {}
    fun generic(s: @Ann(Generic::class) String) {}
    fun innerGeneric(s: @Ann(InnerGeneric.Inner::class) String) {}
}

class Simple
class Generic<T>
class InnerGeneric<A, B> {
    inner class Inner<in C, D : A>
}

// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass

annotation class A
annotation class B(val int: Int)
annotation class C(val int: Int = 42)

annotation class G<T: Any>(val int: KClass<T>)

fun box() {
    val a = A()
    val b = B(4)
    val c = C()
    val foo = G(Int::class)
}

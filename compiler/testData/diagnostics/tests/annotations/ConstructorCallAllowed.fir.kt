// WITH_RUNTIME
// SKIP_TXT
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass

annotation class A
annotation class B(val int: Int)
annotation class C(val int: Int = 42)

annotation class G<T: Any>(val int: KClass<T>)

fun box() {
    val a = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>A()<!>
    val b = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>B(4)<!>
    val c = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>C()<!>
    val foo = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>G(Int::class)<!>
}

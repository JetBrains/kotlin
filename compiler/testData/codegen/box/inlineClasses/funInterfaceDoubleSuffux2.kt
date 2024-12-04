// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: Kt15AbstractMethodErrorRepro.kt

fun box(): String {
    val foo = MyInterface { _ -> }
    foo.myMethod(MyValueClazz(0L))
    return "OK"
}

// FILE: Kt15AbstractMethodError2.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class MyValueClazz(val base: Long)

fun interface MyInterface {
    fun myMethod(x: MyValueClazz)
}
// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR

import java.lang.reflect.InvocationTargetException

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val str: String)

class A(val a: IC, val x : String) {
    fun foo() = "$a$x"

    private constructor(x: IC) : this(x, "")
}

inline fun assertThrowsInvocationTargetException(block: () -> Unit): Boolean {
    try {
        block()
    } catch (t: Throwable) {
        return t is InvocationTargetException
    }
    return false
}

fun box(): String {
    if (!assertThrowsInvocationTargetException { ::A.call(null, "").foo() }) return "Fail 1"
    if (!assertThrowsInvocationTargetException { ::A.call(IC(""), null).foo() }) return "Fail 2"
    val privateConstructor = A::class.constructors.single { it.parameters.size == 1 }
    if (assertThrowsInvocationTargetException { privateConstructor.call(null).foo() }) return "Fail 3"
    return "OK"
}
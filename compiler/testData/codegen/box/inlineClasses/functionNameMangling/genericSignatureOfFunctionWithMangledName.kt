// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR, JS, JS_IR
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.test.*

inline class InlineClass1(val s: String)
inline class InlineClass2(val n: Number)

fun <T : InlineClass1, U : InlineClass2> foo(t: T, u: U) {}

fun box(): String {
    val fooRef: (InlineClass1, InlineClass2) -> Unit = ::foo
    val fooMethod = (fooRef as KFunction<*>).javaMethod!!

    assertEquals("[T, U]", fooMethod.genericParameterTypes.asList().toString())

    return "OK"
}
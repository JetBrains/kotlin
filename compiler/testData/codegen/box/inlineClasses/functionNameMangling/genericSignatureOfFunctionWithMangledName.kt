// WITH_STDLIB
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.test.*

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineClass1(val s: String)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineClass2(val n: Number)

fun <T : InlineClass1, U : InlineClass2> foo(t: T, u: U) {}

fun box(): String {
    val fooRef: (InlineClass1, InlineClass2) -> Unit = ::foo
    val fooMethod = (fooRef as KFunction<*>).javaMethod!!

    assertEquals("[T, U]", fooMethod.genericParameterTypes.asList().toString())

    return "OK"
}
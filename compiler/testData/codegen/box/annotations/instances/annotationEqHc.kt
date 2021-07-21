// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_RUNTIME
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass

annotation class Bar(val i:Int, val s: String, val f: Float)

annotation class Foo(
    val int: Int,
    val s: String,
    val arr: Array<String>,
    val arr2: IntArray,
    val kClass: KClass<*>,
    val bar: Bar
)

data class BarLike(val i:Int, val s: String, val f: Float)

fun box(): String {
    val foo1 = Foo(42, "foo", arrayOf("a", "b"), intArrayOf(1,2), Bar::class, Bar(10, "bar", Float.NaN))
    val foo2 = Foo(42, "foo", arrayOf("a", "b"), intArrayOf(1,2), Bar::class, Bar(10, "bar", Float.NaN))
    if (foo1 != foo2) return "Failed equals"
    val barlike = BarLike(10, "bar", Float.NaN)
    if (barlike.hashCode() != foo1.bar.hashCode()) return "Failed HC1"
    if (barlike.hashCode() != foo2.bar.hashCode()) return "Failed HC2"
    return "OK"
}
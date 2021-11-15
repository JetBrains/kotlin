// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS

// WITH_STDLIB
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

fun makeHC(name: String, value: Any) = (127 * name.hashCode()) xor value.hashCode()

fun box(): String {
    val foo1 = Foo(42, "foo", arrayOf("a", "b"), intArrayOf(1,2), Bar::class, Bar(10, "bar", Float.NaN))
    val foo2 = Foo(42, "foo", arrayOf("a", "b"), intArrayOf(1,2), Bar::class, Bar(10, "bar", Float.NaN))
    if (foo1 != foo2) return "Failed equals ${foo1.toString()} ${foo2.toString()}"
    val barlike = makeHC("i", 10) + makeHC("s", "bar") + makeHC("f", Float.NaN)
    if (barlike != foo1.bar.hashCode()) return "Failed HC1"
    if (barlike != foo2.bar.hashCode()) return "Failed HC2"
    return "OK"
}

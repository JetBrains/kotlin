// FIR_DUMP
// WITH_REFLECT

import kotlin.reflect.KProperty

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
public operator fun <V, V1 : V> Map<in String, @kotlin.internal.Exact V>.getValue(thisRef: Any?, property: KProperty<*>): V1 = null!!

val m2: Map<String, *>  = mapOf("baz" to "bat")
val bar: String get() = m2.getValue(null, ::bar)

fun foo() {
    val m1: Map<String, Any>  = mapOf("foo" to "bar")
    val foo: String by m1
    val baz: String by m2
    println(foo) // bar
    println(baz) // kotlin.KotlinNothingValueException
    println(bar)
}

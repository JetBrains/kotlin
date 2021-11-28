// WITH_STDLIB

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

operator fun <R> KMutableProperty0<R>.setValue(host: Any?, property: KProperty<*>, value: R) = set(value)
operator fun <R> KMutableProperty0<R>.getValue(host: Any?, property: KProperty<*>): R = get()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val i: Int)

var f = Foo(4)

fun modify(ref: KMutableProperty0<Foo>) {
    var a by ref
    a = Foo(1)
}

fun box(): String {
    modify(::f)
    if (f.i != 1) throw AssertionError()

    return "OK"
}
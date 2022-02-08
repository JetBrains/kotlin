// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

operator fun <R> KMutableProperty0<R>.setValue(host: Any?, property: KProperty<*>, value: R) = set(value)
operator fun <R> KMutableProperty0<R>.getValue(host: Any?, property: KProperty<*>): R = get()

OPTIONAL_JVM_INLINE_ANNOTATION
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
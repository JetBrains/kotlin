// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

operator fun <R> KMutableProperty0<R>.setValue(host: Any?, property: KProperty<*>, value: R) = set(value)
operator fun <R> KMutableProperty0<R>.getValue(host: Any?, property: KProperty<*>): R = get()

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Int>(val i: T)

var f = Foo(4)

fun <T: Int> modify(ref: KMutableProperty0<Foo<T>>) {
    var a by ref
    a = Foo(1) as Foo<T>
}

fun box(): String {
    modify(::f)
    if (f.i != 1) throw AssertionError()

    return "OK"
}
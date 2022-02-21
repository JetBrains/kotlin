// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class BoxT<T: Any>(val boxed: T?)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class I32<T: Int>(val value: T): IFoo

fun boxToTypeParameter(x: I32<Int>?) = BoxT(x)
fun boxToNullableAny(x: I32<Int>?) = BoxAny(x)
fun boxToNullableInterface(x: I32<Int>?) = BoxFoo(x)

fun box(): String {
    if (boxToNullableAny(null).boxed != null) throw AssertionError()
    if (boxToTypeParameter(null).boxed != null) throw AssertionError()
    if (boxToNullableInterface(null).boxed != null) throw AssertionError()

    return "OK"
}
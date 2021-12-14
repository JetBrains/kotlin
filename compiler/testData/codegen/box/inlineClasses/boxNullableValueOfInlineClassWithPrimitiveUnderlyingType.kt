// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

class BoxT<T>(val boxed: T)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class I32(val value: Int): IFoo

fun boxToTypeParameter(x: I32?) = BoxT(x)
fun boxToNullableAny(x: I32?) = BoxAny(x)
fun boxToNullableInterface(x: I32?) = BoxFoo(x)

fun box(): String {
    if (boxToNullableAny(null).boxed != null) throw AssertionError()
    if (boxToTypeParameter(null).boxed != null) throw AssertionError()
    if (boxToNullableInterface(null).boxed != null) throw AssertionError()

    return "OK"
}
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class BoxT<T>(val boxed: T)
class BoxT2<T: Any>(val boxed: T?)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class I32<T: Int>(val value: T?) : IFoo

fun <T: Int> boxToTypeParameter(x: I32<T>?) = BoxT(x)
fun <T: Int> boxToTypeParameter2(x: I32<T>?) = BoxT2(x)
fun <T: Int> boxToNullableAny(x: I32<T>?) = BoxAny(x)
fun <T: Int> boxToNullableInterface(x: I32<T>?) = BoxFoo(x)

fun <T: Int> useNullableI32(x: I32<T>?) {
    if (x != null) throw AssertionError()
}

fun box(): String {
    useNullableI32(boxToTypeParameter<Int>(null).boxed)
    useNullableI32(boxToTypeParameter2<Int>(null).boxed)
    useNullableI32(boxToNullableAny<Int>(null).boxed as I32<Int>?)
    useNullableI32(boxToNullableInterface<Int>(null).boxed as I32<Int>?)

    return "OK"
}
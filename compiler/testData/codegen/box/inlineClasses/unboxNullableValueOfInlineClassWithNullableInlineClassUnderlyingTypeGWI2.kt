// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class BoxT<T>(val boxed: T)
class BoxT2<T: Any>(val boxed: T?)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

interface Marker

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt(val i: Int): Marker

OPTIONAL_JVM_INLINE_ANNOTATION
value class I32<T: IcInt>(val value: T?) : IFoo where T: Marker

fun <T: IcInt> boxToTypeParameter(x: I32<T>?) where T: Marker = BoxT(x)
fun <T: IcInt> boxToTypeParameter2(x: I32<T>?) where T: Marker = BoxT2(x)
fun <T: IcInt> boxToNullableAny(x: I32<T>?) where T: Marker = BoxAny(x)
fun <T: IcInt> boxToNullableInterface(x: I32<T>?) where T: Marker = BoxFoo(x)

fun <T: IcInt> useNullableI32(x: I32<T>?) where T: Marker {
    if (x != null) throw AssertionError()
}

fun box(): String {
    useNullableI32(boxToTypeParameter<IcInt>(null).boxed)
    useNullableI32(boxToTypeParameter2<IcInt>(null).boxed)
    useNullableI32(boxToNullableAny<IcInt>(null).boxed as I32<IcInt>?)
    useNullableI32(boxToNullableInterface<IcInt>(null).boxed as I32<IcInt>?)

    return "OK"
}
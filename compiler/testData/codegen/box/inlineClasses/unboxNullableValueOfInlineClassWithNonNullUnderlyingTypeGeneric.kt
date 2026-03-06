// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class BoxT<T>(val boxed: T)
class BoxT2<T: Any>(val boxed: T?)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str<T: String>(val value: T) : IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str2<T: Str<String>>(val value: T): IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class StrArr(val value: Array<String>): IFoo

fun <T: String> boxToTypeParameter(x: Str<T>?) = BoxT(x)
fun <T: String> boxToTypeParameter2(x: Str<T>?) = BoxT2(x)
fun <T: String> boxToNullableAny(x: Str<T>?) = BoxAny(x)
fun <T: String> boxToNullableInterface(x: Str<T>?) = BoxFoo(x)

fun <T: Str<String>> box2ToTypeParameter(x: Str2<T>?) = BoxT(x)
fun <T: Str<String>> box2ToTypeParameter2(x: Str2<T>?) = BoxT2(x)
fun <T: Str<String>> box2ToNullableAny(x: Str2<T>?) = BoxAny(x)
fun <T: Str<String>> box2ToNullableInterface(x: Str2<T>?) = BoxFoo(x)

fun boxArrToTypeParameter(x: StrArr?) = BoxT(x)
fun boxArrToTypeParameter2(x: StrArr?) = BoxT2(x)
fun boxArrToNullableAny(x: StrArr?) = BoxAny(x)
fun boxArrToNullableInterface(x: StrArr?) = BoxFoo(x)

fun <T : String> useNullableStr(x: Str<T>?) {
    if (x != null) throw AssertionError()
}

fun <T: Str<String>> useNullableStr2(x: Str2<T>?) {
    if (x != null) throw AssertionError()
}

fun useNullableStrArr(x: StrArr?) {
    if (x != null) throw AssertionError()
}

fun box(): String {
    useNullableStr(boxToTypeParameter<String>(null).boxed)
    useNullableStr(boxToTypeParameter2<String>(null).boxed)
    useNullableStr(boxToNullableAny<String>(null).boxed as Str<String>?)
    useNullableStr(boxToNullableInterface<String>(null).boxed as Str<String>?)

    useNullableStr2(box2ToTypeParameter<Str<String>>(null).boxed)
    useNullableStr2(box2ToTypeParameter2<Str<String>>(null).boxed)
    useNullableStr2(box2ToNullableAny<Str<String>>(null).boxed as Str2<Str<String>>?)
    useNullableStr2(box2ToNullableInterface<Str<String>>(null).boxed as Str2<Str<String>>?)

    useNullableStrArr(boxArrToTypeParameter(null).boxed)
    useNullableStrArr(boxArrToTypeParameter2(null).boxed)
    useNullableStrArr(boxArrToNullableAny(null).boxed as StrArr?)
    useNullableStrArr(boxArrToNullableInterface(null).boxed as StrArr?)

    return "OK"
}
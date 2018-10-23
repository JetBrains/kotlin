// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

class BoxT<T>(val boxed: T)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

inline class Str(val value: String) : IFoo

inline class Str2(val value: Str): IFoo

inline class StrArr(val value: Array<String>): IFoo

fun boxToTypeParameter(x: Str?) = BoxT(x)
fun boxToNullableAny(x: Str?) = BoxAny(x)
fun boxToNullableInterface(x: Str?) = BoxFoo(x)

fun box2ToTypeParameter(x: Str2?) = BoxT(x)
fun box2ToNullableAny(x: Str2?) = BoxAny(x)
fun box2ToNullableInterface(x: Str2?) = BoxFoo(x)

fun boxArrToTypeParameter(x: StrArr?) = BoxT(x)
fun boxArrToNullableAny(x: StrArr?) = BoxAny(x)
fun boxArrToNullableInterface(x: StrArr?) = BoxFoo(x)

fun useNullableStr(x: Str?) {
    if (x != null) throw AssertionError()
}

fun useNullableStr2(x: Str2?) {
    if (x != null) throw AssertionError()
}

fun useNullableStrArr(x: StrArr?) {
    if (x != null) throw AssertionError()
}

fun box(): String {
    useNullableStr(boxToTypeParameter(null).boxed)
    useNullableStr(boxToNullableAny(null).boxed as Str?)
    useNullableStr(boxToNullableInterface(null).boxed as Str?)

    useNullableStr2(box2ToTypeParameter(null).boxed)
    useNullableStr2(box2ToNullableAny(null).boxed as Str2?)
    useNullableStr2(box2ToNullableInterface(null).boxed as Str2?)

    useNullableStrArr(boxArrToTypeParameter(null).boxed)
    useNullableStrArr(boxArrToNullableAny(null).boxed as StrArr?)
    useNullableStrArr(boxArrToNullableInterface(null).boxed as StrArr?)

    return "OK"
}
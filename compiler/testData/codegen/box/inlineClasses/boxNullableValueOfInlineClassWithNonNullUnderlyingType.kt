// WITH_STDLIB

class BoxT<T>(val boxed: T)
class BoxAny(val boxed: Any?)
class BoxFoo(val boxed: IFoo?)

interface IFoo

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Str(val value: String) : IFoo

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Str2(val value: Str): IFoo

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class StrArr(val value: Array<String>): IFoo

fun boxToTypeParameter(x: Str?) = BoxT(x)
fun boxToNullableAny(x: Str?) = BoxAny(x)
fun boxToNullableInterface(x: Str?) = BoxFoo(x)

fun box2ToTypeParameter(x: Str2?) = BoxT(x)
fun box2ToNullableAny(x: Str2?) = BoxAny(x)
fun box2ToNullableInterface(x: Str2?) = BoxFoo(x)

fun boxArrToTypeParameter(x: StrArr?) = BoxT(x)
fun boxArrToNullableAny(x: StrArr?) = BoxAny(x)
fun boxArrToNullableInterface(x: StrArr?) = BoxFoo(x)

fun box(): String {
    if (boxToNullableAny(null).boxed != null) throw AssertionError()
    if (boxToTypeParameter(null).boxed != null) throw AssertionError()
    if (boxToNullableInterface(null).boxed != null) throw AssertionError()

    if (box2ToNullableAny(null).boxed != null) throw AssertionError()
    if (box2ToTypeParameter(null).boxed != null) throw AssertionError()
    if (box2ToNullableInterface(null).boxed != null) throw AssertionError()

    if (boxArrToNullableAny(null).boxed != null) throw AssertionError()
    if (boxArrToTypeParameter(null).boxed != null) throw AssertionError()
    if (boxArrToNullableInterface(null).boxed != null) throw AssertionError()

    return "OK"
}
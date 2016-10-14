package rendererTest

public fun pub() {}

internal fun int: String {}

fun int2(vararg ints: Int): Int = 5

private fun prv(a: String, b: Int = 5) = 5

public fun Int.ext(): Int {}

public fun <out T> withTypeParam(a: Array<T>): Int {}

interface Foo
interface Bar

fun <P> funTypeParameterWithTwoUpperBounds() where P : Foo, P : Bar = 17

@Deprecated("") fun deprecatedFun()

//package rendererTest
//public fun pub(): kotlin.Unit defined in rendererTest
//internal fun int(): kotlin.String defined in rendererTest
//public fun int2(vararg ints: kotlin.Int): kotlin.Int defined in rendererTest
//value-parameter vararg ints: kotlin.Int defined in rendererTest.int2
//private fun prv(a: kotlin.String, b: kotlin.Int = ...): kotlin.Int defined in rendererTest
//value-parameter a: kotlin.String defined in rendererTest.prv
//value-parameter b: kotlin.Int = ... defined in rendererTest.prv
//public fun kotlin.Int.ext(): kotlin.Int defined in rendererTest
//public fun <out T> withTypeParam(a: kotlin.Array<T>): kotlin.Int defined in rendererTest
//<out T> defined in rendererTest.withTypeParam
//value-parameter a: kotlin.Array<T> defined in rendererTest.withTypeParam
//public interface Foo defined in rendererTest
//public interface Bar defined in rendererTest
//public fun <P : rendererTest.Foo> funTypeParameterWithTwoUpperBounds(): kotlin.Int where P : rendererTest.Bar defined in rendererTest
//<P : rendererTest.Foo & rendererTest.Bar> defined in rendererTest.funTypeParameterWithTwoUpperBounds
//@kotlin.Deprecated(message = "") public fun deprecatedFun(): kotlin.Unit defined in rendererTest

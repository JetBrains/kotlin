package rendererTest

public fun pub() {}

internal fun int : String {}

fun int2(vararg ints : Int) : Int = 5

private fun prv(a : String, b : Int = 5) = 5

public fun Int.ext() : Int {}

public fun <out T> withTypeParam(a : Array<T>) : Int {}

trait Foo
trait Bar

fun <P> funTypeParameterWithTwoUpperBounds() where P : Foo, P : Bar = 17

//package rendererTest defined in root package
//public final fun pub() : Unit defined in rendererTest
//internal final fun int() : jet.String defined in rendererTest
//internal final fun int2(vararg ints : jet.Int) : jet.Int defined in rendererTest
//value-parameter vararg val ints : jet.Int defined in rendererTest.int2
//private final fun prv(a : jet.String, b : jet.Int = ...) : jet.Int defined in rendererTest
//value-parameter val a : jet.String defined in rendererTest.prv
//value-parameter val b : jet.Int defined in rendererTest.prv
//public final fun jet.Int.ext() : jet.Int defined in rendererTest
//public final fun <out T> withTypeParam(a : jet.Array<T>) : jet.Int defined in rendererTest
//<out T> defined in rendererTest.withTypeParam
//value-parameter val a : jet.Array<T> defined in rendererTest.withTypeParam
//internal trait Foo defined in rendererTest
//internal trait Bar defined in rendererTest
//internal final fun <P> funTypeParameterWithTwoUpperBounds() : jet.Int where P : rendererTest.Foo, P : rendererTest.Bar defined in rendererTest
//<P : rendererTest.Foo & rendererTest.Bar> defined in rendererTest.funTypeParameterWithTwoUpperBounds

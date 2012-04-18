package rendererTest

public fun pub() {}

internal fun int : String {}

fun int2(vararg ints : Int) : Int = 5

private fun prv(a : String, b : Int = 5) = 5

public fun Int.ext() : Int {}

//package rendererTest defined in root package
//public final fun pub() : Unit defined in rendererTest
//internal final fun int() : jet.String defined in rendererTest
//internal final fun int2(val ints : jet.IntArray) : jet.Int defined in rendererTest
//value-parameter vararg val ints : jet.IntArray defined in rendererTest.int2
//private final fun prv(val a : jet.String, val b : jet.Int = ...) : jet.Int defined in rendererTest
//value-parameter val a : jet.String defined in rendererTest.prv
//value-parameter val b : jet.Int defined in rendererTest.prv
//public final fun jet.Int.ext() : jet.Int defined in rendererTest
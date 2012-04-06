package rendererTest

public fun pub() {}

internal fun int : String {}

fun int2() : Int = 5

private fun prv() = 5

public fun Int.ext() : Int {}

//package rendererTest defined in <module>.<root>
//public final fun pub() : Unit defined in <module>.<root>.rendererTest
//internal final fun int() : jet.String defined in <module>.<root>.rendererTest
//internal final fun int2() : jet.Int defined in <module>.<root>.rendererTest
//private final fun prv() : jet.Int defined in <module>.<root>.rendererTest
//public final fun jet.Int.ext() : jet.Int defined in <module>.<root>.rendererTest
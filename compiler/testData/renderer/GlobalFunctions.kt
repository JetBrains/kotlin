package rendererTest

public fun pub() {}

internal fun int : String {}

val int2 : Int = 5

private fun prv() = 5

public fun Int.ext() : Int {}

//package rendererTest defined in <module>.<root>
//final fun pub() : Unit defined in <module>.<root>.rendererTest
//final fun int() : jet.String defined in <module>.<root>.rendererTest
//final val int2 : jet.Int defined in <module>.<root>.rendererTest
//final fun prv() : jet.Int defined in <module>.<root>.rendererTest
//final fun jet.Int.ext() : jet.Int defined in <module>.<root>.rendererTest
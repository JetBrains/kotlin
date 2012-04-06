package rendererTest

public val pub = ""

internal var int : String = ""

val int2 : Int = 5

private var private = 5

public val Int.ext : Int
get() {}

//package rendererTest defined in <module>.<root>
//final val pub : jet.String defined in <module>.<root>.rendererTest
//final var int : jet.String defined in <module>.<root>.rendererTest
//final val int2 : jet.Int defined in <module>.<root>.rendererTest
//final var private : jet.Int defined in <module>.<root>.rendererTest
//final val jet.Int.ext : jet.Int defined in <module>.<root>.rendererTest
//final fun jet.Int.get-ext() : jet.Int defined in <module>.<root>.rendererTest
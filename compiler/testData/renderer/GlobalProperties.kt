package rendererTest

public val pub = ""

internal var int : String = ""

val int2 : Int = 5

private var private = 5

public val Int.ext : Int
get() {}

//package rendererTest defined in <module>.<root>
//public final val pub : jet.String defined in <module>.<root>.rendererTest
//internal final var int : jet.String defined in <module>.<root>.rendererTest
//internal final val int2 : jet.Int defined in <module>.<root>.rendererTest
//private final var private : jet.Int defined in <module>.<root>.rendererTest
//public final val jet.Int.ext : jet.Int defined in <module>.<root>.rendererTest
//public final fun jet.Int.get-ext() : jet.Int defined in <module>.<root>.rendererTest
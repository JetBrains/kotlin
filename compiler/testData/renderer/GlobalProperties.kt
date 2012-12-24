package rendererTest

public val pub = ""

internal var int : String = ""

val int2 : Int = 5

private var private = 5

public val Int.ext : Int
get() {}

deprecated("") val deprecatedVal = 5

//package rendererTest defined in root package
//public final val pub : jet.String defined in rendererTest
//internal final var int : jet.String defined in rendererTest
//internal final val int2 : jet.Int defined in rendererTest
//private final var private : jet.Int defined in rendererTest
//public final val jet.Int.ext : jet.Int defined in rendererTest
//public final fun jet.Int.<get-ext>() : jet.Int defined in rendererTest
//jet.deprecated internal final val deprecatedVal : jet.Int defined in rendererTest

package rendererTest

public val pub = ""

internal var int : String = ""

val int2 : Int = 5

private var private = 5

public val Int.ext : Int
get() {}

deprecated("") val deprecatedVal = 5

public val <T> T.extWithTwoUpperBounds: Int where T: CharSequence, T: Number
get() {}

//package rendererTest defined in root package
//public final val pub : jet.String defined in rendererTest
//internal final var int : jet.String defined in rendererTest
//internal final val int2 : jet.Int defined in rendererTest
//private final var private : jet.Int defined in rendererTest
//public final val jet.Int.ext : jet.Int defined in rendererTest
//public final fun jet.Int.<get-ext>() : jet.Int defined in rendererTest
//jet.deprecated internal final val deprecatedVal : jet.Int defined in rendererTest
//public final val <T> T.extWithTwoUpperBounds : jet.Int where T : jet.CharSequence, T : jet.Number defined in rendererTest
//<T : jet.CharSequence & jet.Number> defined in rendererTest
//public final fun T.<get-extWithTwoUpperBounds>() : jet.Int defined in rendererTest
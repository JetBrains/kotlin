package rendererTest

public val pub = ""

internal var int: String = ""

val int2: Int = 5

private var private = 5

public val Int.ext: Int
get() {}

deprecated("") val deprecatedVal = 5

public val <T> T.extWithTwoUpperBounds: Int where T : CharSequence, T : Number
get() {}

//package rendererTest
//public val pub: jet.String defined in rendererTest
//internal var int: jet.String defined in rendererTest
//internal val int2: jet.Int defined in rendererTest
//private var private: jet.Int defined in rendererTest
//public val jet.Int.ext: jet.Int defined in rendererTest
//public fun jet.Int.<get-ext>(): jet.Int defined in rendererTest
//jet.deprecated internal val deprecatedVal: jet.Int defined in rendererTest
//public val <T : jet.CharSequence> T.extWithTwoUpperBounds: jet.Int where T : jet.Number defined in rendererTest
//<T : jet.CharSequence & jet.Number> defined in rendererTest
//public fun T.<get-extWithTwoUpperBounds>(): jet.Int defined in rendererTest

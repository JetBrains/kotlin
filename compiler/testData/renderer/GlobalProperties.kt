package rendererTest

public val pub = ""

internal var int: String = ""

val int2: Int = 5

private var private = 5

public val Int.ext: Int
get() {}

@Deprecated("") val deprecatedVal = 5

public val <T> T.extWithTwoUpperBounds: Int where T : CharSequence, T : Number
get() {}

//package rendererTest
//public val pub: kotlin.String defined in rendererTest
//internal var int: kotlin.String defined in rendererTest
//public val int2: kotlin.Int defined in rendererTest
//private var private: kotlin.Int defined in rendererTest
//public val kotlin.Int.ext: kotlin.Int defined in rendererTest
//public fun kotlin.Int.<get-ext>(): kotlin.Int defined in rendererTest
//@kotlin.Deprecated(message = "") public val deprecatedVal: kotlin.Int defined in rendererTest
//public val <T : kotlin.CharSequence> T.extWithTwoUpperBounds: kotlin.Int where T : kotlin.Number defined in rendererTest
//<T : kotlin.CharSequence & kotlin.Number> defined in rendererTest.extWithTwoUpperBounds
//public fun T.<get-extWithTwoUpperBounds>(): kotlin.Int defined in rendererTest

@Deprecated("", level = DeprecationLevel.WARNING)
fun Int.funWarning() {}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun Int.funHidden() {}

@Deprecated("", level = DeprecationLevel.ERROR)
fun Int.funError() {}

@Deprecated("", level = DeprecationLevel.WARNING)
val Int.propWarning: Int
    get() = 1

@Deprecated("", level = DeprecationLevel.HIDDEN)
val Int.propHidden: Int
    get() = 1

@Deprecated("", level = DeprecationLevel.ERROR)
val Int.propError: Int
    get() = 1

/**
 * [Int.funWar<caret_1>ning]
 * [Int.funHid<caret_2>den]
 * [Int.fun<caret_3>Error]
 * [Int.propWarn<caret_4>ing]
 * [Int.propHid<caret_5>den]
 * [Int.propEr<caret_6>ror]
 */
fun usage() {}
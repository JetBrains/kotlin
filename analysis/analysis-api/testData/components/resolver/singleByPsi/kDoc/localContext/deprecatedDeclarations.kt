@Deprecated("", level = DeprecationLevel.WARNING)
fun funWarning() {}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun funHidden() {}

@Deprecated("", level = DeprecationLevel.ERROR)
fun funError() {}


@Deprecated("", level = DeprecationLevel.WARNING)
class ClassWarning

@Deprecated("", level = DeprecationLevel.HIDDEN)
class ClassHidden() {}

@Deprecated("", level = DeprecationLevel.ERROR)
class ClassError() {}

@Deprecated("", level = DeprecationLevel.WARNING)
val propWarning: Int = 0

@Deprecated("", level = DeprecationLevel.HIDDEN)
val propHidden: Int = 0

@Deprecated("", level = DeprecationLevel.ERROR)
val propError: Int = 0

/**
 * [funWar<caret_1>ning]
 * [funHid<caret_2>den]
 * [fun<caret_3>Error]
 * [ClassWar<caret_4>ning]
 * [ClassHid<caret_5>den]
 * [ClassEr<caret_6>ror]
 * [propWarn<caret_7>ing]
 * [propHid<caret_8>den]
 * [propEr<caret_9>ror]
 */
fun usage() {}
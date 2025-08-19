/**
 * [conPropWar<caret_1>ning]
 * [conProp<caret_2>Error]
 * [conProp<caret_3>Hidden]
 * [propW<caret_4>arning]
 * [propEr<caret_5>ror]
 * [propHi<caret_6>dden]
 */
class AA(
    @Deprecated("", level = DeprecationLevel.WARNING)
    val conPropWarning: Int,
    @Deprecated("", level = DeprecationLevel.ERROR)
    val conPropError: Int,
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    val conPropHidden: Int,
) {
    @Deprecated("", level = DeprecationLevel.WARNING)
    val propWarning: Int = 0

    @Deprecated("", level = DeprecationLevel.ERROR)
    val propError: Int = 0

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    val propHidden: Int = 0

    /**
     * [A<caret_7>A]
     * [hidde<caret_8>nConPar]
     */
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    constructor(hiddenConPar: Int) : this(0, 0, 0)
}
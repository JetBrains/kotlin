/**
 * @property [conPropWar<caret_1>ning]
 * @property [conProp<caret_2>Error]
 * @property [conProp<caret_3>Hidden]
 * @property [propW<caret_4>arning]
 * @property [propEr<caret_5>ror]
 * @property [propHi<caret_6>dden]
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
}
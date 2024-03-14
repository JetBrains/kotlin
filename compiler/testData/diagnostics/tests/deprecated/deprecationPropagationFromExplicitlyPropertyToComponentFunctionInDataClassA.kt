// FIR_IDENTICAL
// LANGUAGE: -PropagatePropertyDeprecationToComponentFunctionInDataClass

data class DataKlass(
    val supported: Int,
    @property:Deprecated("deprecated with default deprecation level")
    val deprecated: Int,
    @property:Deprecated("deprecated with a warning", level = DeprecationLevel.WARNING)
    val deprecatedWithWarning: Int,
    @property:Deprecated("deprecated with an error", level = DeprecationLevel.ERROR)
    val deprecatedWithError: Int,
    @property:Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    val deprecatedAndHidden: Int,
)

fun test(arg: DataKlass) {
    val a = arg.component1()
    val b = arg.component2()
    val c = arg.component3()
    val d = arg.component4()
    val e = arg.component5()
}

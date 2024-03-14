// LANGUAGE: -PropagatePropertyDeprecationToComponentFunctionInDataClass

data class DataKlass(
    val supported: Int,
    @Deprecated("deprecated with default deprecation level")
    val deprecated: Int,
    @Deprecated("deprecated with a warning", level = DeprecationLevel.WARNING)
    val deprecatedWithWarning: Int,
    @Deprecated("deprecated with an error", level = DeprecationLevel.ERROR)
    val deprecatedWithError: Int,
    @Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    val deprecatedAndHidden: Int,
)

fun test(arg: DataKlass) {
    val a = arg.component1()
    val b = arg.<!DEPRECATION!>component2<!>()
    val c = arg.<!DEPRECATION!>component3<!>()
    val d = arg.<!DEPRECATION_FROM_DATA_CLASS_PROPERTY!>component4<!>()
    val e = arg.<!DEPRECATION_FROM_DATA_CLASS_PROPERTY!>component5<!>()
}

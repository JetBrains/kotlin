// LANGUAGE: +PropagatePropertyDeprecationToComponentFunctionInDataClass

class SimpleKlass {
    operator fun component1(): Int = 42
    @Deprecated("deprecated with default deprecation level")
    operator fun component2(): Int = 42
    @Deprecated("deprecated with a warning", level = DeprecationLevel.WARNING)
    operator fun component3(): Int = 42
    @Deprecated("deprecated with an error", level = DeprecationLevel.ERROR)
    operator fun component4(): Int = 42
    @Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    operator fun component5(): Int = 42
}

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

fun test(simpleKlass: SimpleKlass, dataKlass: DataKlass) {
    val (s1, <!DEPRECATION!>s2<!>, <!DEPRECATION!>s3<!>, <!DEPRECATION_ERROR!>s4<!>, s5) = <!COMPONENT_FUNCTION_MISSING!>simpleKlass<!>
    val (d1, d2, d3, d4, d5) = dataKlass
    val (_, <!DEPRECATION!>_<!>, <!DEPRECATION!>_<!>, <!DEPRECATION_ERROR!>_<!>, _) = <!COMPONENT_FUNCTION_MISSING!>simpleKlass<!>
    val (_, _, _, _, _) = dataKlass
}

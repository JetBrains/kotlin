// FIR_IDENTICAL
@Deprecated("", level = DeprecationLevel.ERROR)
class DeprecatedClass

typealias AliasOfDeprecated = <!DEPRECATION_ERROR!>DeprecatedClass<!>

@RequiresOptIn("", RequiresOptIn.Level.ERROR)
annotation class MyOptIn

@MyOptIn
class OptInClass

typealias AliasOfOptIn = <!OPT_IN_USAGE_ERROR!>OptInClass<!>

fun test(
    @Suppress("DEPRECATION_ERROR")
    dc: DeprecatedClass,
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    adc: AliasOfDeprecated,
    @Suppress("OPT_IN_USAGE_ERROR")
    oi: OptInClass,
    @Suppress("OPT_IN_USAGE_ERROR")
    aoi: AliasOfOptIn,
) {}

interface I {
    @MyOptIn
    fun foo()
}

class C : I {
    @Suppress("OPT_IN_OVERRIDE_ERROR")
    override fun foo() {}
}

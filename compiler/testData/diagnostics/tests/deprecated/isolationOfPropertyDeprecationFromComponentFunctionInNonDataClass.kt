// FIR_IDENTICAL

class SimpleKlass(
    val supported: Int,
    @Deprecated("deprecated with default deprecation level")
    val deprecated: Int,
    @Deprecated("deprecated with a warning", level = DeprecationLevel.WARNING)
    val deprecatedWithWarning: Int,
    @Deprecated("deprecated with an error", level = DeprecationLevel.ERROR)
    val deprecatedWithError: Int,
    @Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    val deprecatedAndHidden: Int,
) {
    operator fun component1(): Int = supported
    operator fun component2(): Int = <!DEPRECATION!>deprecated<!>
    operator fun component3(): Int = <!DEPRECATION!>deprecatedWithWarning<!>
    operator fun component4(): Int = <!DEPRECATION_ERROR!>deprecatedWithError<!>
    operator fun component5(): Int = <!UNRESOLVED_REFERENCE!>deprecatedAndHidden<!>
}

fun test(arg: SimpleKlass) {
    val a = arg.component1()
    val b = arg.component2()
    val c = arg.component3()
    val d = arg.component4()
    val e = arg.component5()
}

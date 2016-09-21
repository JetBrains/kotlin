// !DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("", level = DeprecationLevel.HIDDEN)
open class Foo

fun test(f: <!DEPRECATION_ERROR!>Foo<!>) {
    f.toString()
    val g: <!DEPRECATION_ERROR!>Foo<!>? = <!UNRESOLVED_REFERENCE!>Foo<!>()
}

class Bar : <!UNRESOLVED_REFERENCE, DEPRECATION_ERROR, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Foo<!>()

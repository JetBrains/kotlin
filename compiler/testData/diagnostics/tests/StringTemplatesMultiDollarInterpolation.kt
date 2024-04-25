// FIR_IDENTICAL
// FIR_DUMP
// !LANGUAGE: +MultiDollarInterpolation
fun demo() {
    fun f(x: String) { }
    val abc = 1

    $$"$"

    $$"$abc"
    $$"$$abc"
    $$"this is $abc"
    $$"this is $$abc"
    $$"this is ${abc}"
    $$"this is $${abc}"

    $$"""
    This is a dollar $
    This is a literal $abc
    This is a literal ${abc}
    This is an interpolation $$abc
    This is an interpolation $${abc}
    """

    $$"$def"
    $$"$$<!UNRESOLVED_REFERENCE!>def<!>"
    $$"this is $def"
    $$"this is $$<!UNRESOLVED_REFERENCE!>def<!>"
    $$"this is ${def}"
    $$"this is $${<!UNRESOLVED_REFERENCE!>def<!>}"

    $$"""
    This is a dollar $
    This is a literal $def
    This is a literal ${def}
    This is an interpolation $$<!UNRESOLVED_REFERENCE!>def<!>
    This is an interpolation $${<!UNRESOLVED_REFERENCE!>def<!>}
    """

    $$"${f(def)}"
    $$"$${f(<!UNRESOLVED_REFERENCE!>def<!>)}"

    $$"$${f("$<!UNRESOLVED_REFERENCE!>def<!>")}"
    $$"$${f($$"$def")}"
    $$"$${f($$$"$$def")}"
    $$"$${f($$$"$$$<!UNRESOLVED_REFERENCE!>def<!>")}"

    $$"""
    This is nested interpolation $${f("$<!UNRESOLVED_REFERENCE!>def<!>")}
    And another one $${f($$$"$$$<!UNRESOLVED_REFERENCE!>def<!>")}
    """
}

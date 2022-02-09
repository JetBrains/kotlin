// ISSUE: KT-45043
// DIAGNOSTICS: -UNUSED_PARAMETER

private class Bar

sealed class SealedFoo(
    val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_WARNING!>x<!>: Bar,
    private val y: Bar,
    z: Bar
)

abstract class AbstractFoo(
    <!EXPOSED_PARAMETER_TYPE!>val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_WARNING!>x<!>: Bar<!>,
    <!EXPOSED_PARAMETER_TYPE!>private val y: Bar<!>,
    <!EXPOSED_PARAMETER_TYPE!>z: Bar<!>
)

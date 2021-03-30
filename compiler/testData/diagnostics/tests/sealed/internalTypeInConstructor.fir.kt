// ISSUE: KT-45033
// DIAGNOSTICS: -UNUSED_PARAMETER

internal class Bar

sealed class Foo(
    <!EXPOSED_PARAMETER_TYPE!>internal val x: Bar<!>,
    <!EXPOSED_PARAMETER_TYPE!>y: Bar<!>
)

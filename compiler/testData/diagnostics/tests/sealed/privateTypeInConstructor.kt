// ISSUE: KT-45043, KT-51229
// DIAGNOSTICS: -UNUSED_PARAMETER

private class Bar

sealed class SealedFoo(
    val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_WARNING!>x<!>: Bar,
    private val y: Bar,
    z: Bar
)

abstract class AbstractFoo(
    <!EXPOSED_PARAMETER_TYPE!>val x: Bar<!>,
    <!EXPOSED_PARAMETER_TYPE!>private val y: Bar<!>,
    <!EXPOSED_PARAMETER_TYPE!>z: Bar<!>
)

internal sealed class A {
    protected abstract val b: B?
    protected data class B(val s: String)
    internal data class C private constructor(override val b: B?) : A() {
        constructor() : this(null)
    }
}

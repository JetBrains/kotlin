// FIR_IDENTICAL
// ISSUE: KT-64609

package second

annotation class Anno

class SimpleVarClass(
    @Anno
    @get:Anno
    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@set:Anno<!>
    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@setparam:Anno<!>
    val constructorVariableWithAnnotations: Long,
) {
    @Anno
    @get:Anno
    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@set:Anno<!>
    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@setparam:Anno<!>
    val memberVariableWithAnnotations: Long = 0L
}

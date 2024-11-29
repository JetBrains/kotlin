// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-64609
// LATEST_LV_DIFFERENCE

package second

annotation class Anno

class SimpleVarClass(
    <!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@Anno<!>
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

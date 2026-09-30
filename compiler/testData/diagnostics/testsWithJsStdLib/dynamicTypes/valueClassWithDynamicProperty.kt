// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses

value class Single(val d: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>dynamic<!>)

value class Multi(val d: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>dynamic<!>, val i: Int)

abstract value class Abstract(d: dynamic)

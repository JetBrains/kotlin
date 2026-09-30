// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses

value class Single(val d: dynamic)

value class Multi(val d: dynamic, val i: Int)

abstract value class Abstract(d: dynamic)

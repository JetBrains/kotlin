open class Required

trait A : <!TRAIT_WITH_SUPERCLASS!>Required<!>

val a = <!UNMET_TRAIT_REQUIREMENT!>object<!> : A {}
val b: A = object : A, Required() {}

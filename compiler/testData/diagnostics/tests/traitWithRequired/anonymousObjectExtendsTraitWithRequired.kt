open class Required

trait A : Required

val a = <!UNMET_TRAIT_REQUIREMENT!>object<!> : A {}
val b: A = object : A, Required() {}

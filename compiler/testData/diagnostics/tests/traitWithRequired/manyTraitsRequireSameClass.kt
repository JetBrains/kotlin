open class Required

trait A : Required

trait B : A, Required

trait C : Required

trait D : B, Required

<!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>class W<!> : D
class X : D, Required()
<!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>class Y<!> : C, D
class Z : D, C, Required()

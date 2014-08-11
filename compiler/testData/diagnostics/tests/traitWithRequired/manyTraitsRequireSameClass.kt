open class Required

trait A : Required

trait B : A, Required

trait C : Required

trait D : B, Required

class <!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>W<!> : D
class X : D, Required()
class <!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>Y<!> : C, D
class Z : D, C, Required()

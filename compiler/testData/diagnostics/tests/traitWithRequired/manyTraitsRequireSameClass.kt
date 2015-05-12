open class Required

interface A : <!TRAIT_WITH_SUPERCLASS!>Required<!>

interface B : A, <!TRAIT_WITH_SUPERCLASS!>Required<!>

interface C : <!TRAIT_WITH_SUPERCLASS!>Required<!>

interface D : B, <!TRAIT_WITH_SUPERCLASS!>Required<!>

<!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>class W<!> : D
class X : D, Required()
<!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>class Y<!> : C, D
class Z : D, C, Required()

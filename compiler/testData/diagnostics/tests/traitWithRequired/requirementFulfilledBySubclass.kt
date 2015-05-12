open class RequiredBase

interface Trait : <!TRAIT_WITH_SUPERCLASS!>RequiredBase<!>

open class RequiredDerived : RequiredBase()

<!UNMET_TRAIT_REQUIREMENT!>class A<!> : Trait
class B : Trait, RequiredDerived()

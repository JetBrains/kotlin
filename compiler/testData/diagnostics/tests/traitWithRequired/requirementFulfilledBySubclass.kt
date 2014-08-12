open class RequiredBase

trait Trait : RequiredBase

open class RequiredDerived : RequiredBase()

<!UNMET_TRAIT_REQUIREMENT!>class A<!> : Trait
class B : Trait, RequiredDerived()

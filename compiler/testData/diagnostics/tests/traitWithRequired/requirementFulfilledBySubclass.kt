open class RequiredBase

trait Trait : RequiredBase

open class RequiredDerived : RequiredBase()

class <!UNMET_TRAIT_REQUIREMENT!>A<!> : Trait
class B : Trait, RequiredDerived()

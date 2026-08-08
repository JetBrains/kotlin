// A super type call is split between two structure elements: the call arguments belong to the primary constructor, as FIR stores the
// delegated constructor call there, while the super class type reference belongs to the class itself.

open class Base(parameter: Int)

class WithExplicitPrimaryConstructor(argument: String) : Base(argument)

class WithUnresolvedSuperType(argument: String) : MissingBase(argument)

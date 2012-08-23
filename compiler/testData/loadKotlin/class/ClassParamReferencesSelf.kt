package test

trait TraitWithP<P>

class ClassParamReferencesSelf<A : TraitWithP<A>>

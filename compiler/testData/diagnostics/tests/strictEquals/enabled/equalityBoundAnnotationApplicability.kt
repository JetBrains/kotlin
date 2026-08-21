// RUN_PIPELINE_TILL: FRONTEND

class Regular {
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

class RegularAny {
    override fun equals(@EqualityBound(Any::class) other: Any?): Boolean = true
}

class RegularJLObject {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>java.lang.Object<!>::class) other: Any?): Boolean = true
}

// ---

interface A
interface B
interface AB : A, B

typealias AliasA = A

class MultipleBoundsA : A, B {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class MultipleBoundsB : A, B {
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
}

class EqualityBoundIsSubtype : A, B {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>AB<!>::class) other: Any?): Boolean = true
}

class IntermediateMultipleBoundsA : AB {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class IntermediateMultipleBoundsB : AB {
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
}

class AliasedBound : A {
    override fun equals(@EqualityBound(AliasA::class) other: Any?): Boolean = true
}

class AliasedSupertype : AliasA {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class AliasedBoundAndSupertype : AliasA {
    override fun equals(@EqualityBound(AliasA::class) other: Any?): Boolean = true
}

// ---

open class Child {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>Parent<!>::class) other: Any?): Boolean = true
}

open class NothingBound {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>Nothing<!>::class) other: Any?): Boolean = true
}

class Parent : Child()

// ---

class Unrelated

class UnrelatedBound {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>Unrelated<!>::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override, typeAliasDeclaration */

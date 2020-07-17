package test

interface Base {
    fun foo() {}
}

interface Derived : Base

interface BaseWithProperty {
    val prop: String
        get() = "123"
}

interface DerivedWithProperty : BaseWithProperty

interface Empty

interface EmptyWithNested {
    interface Nested
}

interface WithAbstractDeclaration {
    fun foo()
    var prop: String
}

interface DerivedFromWithAbstractDeclaration : WithAbstractDeclaration
/* NonReanalyzableNonClassDeclarationStructureElement */open class A
    (init: A.() -> Unit)
{
    val prop: String = ""
}

class B() : A()

object C : A(
    {
        fun foo() = B.prop.toString()
    }
) {

}

val f = object : A(
    {
        fun bar() = B.prop.toString()
    }
) {

}

class D : A(
    {
        fun foo() = B.prop.toString()
    }
) {
    constructor(): super(
        {
            fun boo() = prop.toString()
        }
    )
}

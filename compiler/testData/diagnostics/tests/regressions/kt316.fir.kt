// KT-316 Members of traits must be open by default

interface B {
    fun bar() {}
    fun foo() {}
}

open class A() : B{
    override fun foo() {}
}

//KT-559 Forbid abstract method call through super

package kt559

abstract class A {
    abstract val i : Int

    abstract fun foo() : Int

    fun fff() {}
}

abstract class D(): A() {
    override val i : Int = 34
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class C<!>() : D() {
    fun test() {
        super.i
    }
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!>() : A() {
    override fun foo(): Int {
        super.<!ABSTRACT_SUPER_CALL!>i<!>

        super.fff() //everything is ok
        return super.<!ABSTRACT_SUPER_CALL!>foo<!>()  //no error!!
    }
}
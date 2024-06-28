// FIR_IDENTICAL
package override.normal

interface MyTrait {
    fun foo()
    val pr : Unit
}

abstract class MyAbstractClass {
    abstract fun bar()
    abstract val prr : Unit

}

open class MyClass() : MyTrait, MyAbstractClass() {
    override fun foo() {}
    override fun bar() {}

    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

class MyChildClass() : MyClass() {}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class MyIllegalClass<!> : MyTrait, MyAbstractClass() {}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class MyIllegalClass2<!>() : MyTrait, MyAbstractClass() {
    override fun foo() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MyIllegalClass3<!>() : MyTrait, MyAbstractClass() {
    override fun bar() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class MyIllegalClass4<!>() : MyTrait, MyAbstractClass() {
    fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>() {}
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val <!VIRTUAL_MEMBER_HIDDEN!>pr<!> : Unit<!>
    <!NOTHING_TO_OVERRIDE!>override<!> fun other() {}
    <!NOTHING_TO_OVERRIDE!>override<!> val otherPr : Int = 1
}

class MyChildClass1() : MyClass() {
    fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>() {}
    val <!VIRTUAL_MEMBER_HIDDEN!>pr<!> : Unit = Unit
    override fun bar() {}
    override val prr : Unit = Unit
}

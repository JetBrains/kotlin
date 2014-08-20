package override.normal

trait MyTrait {
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

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass<!> : MyTrait, MyAbstractClass() {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass2<!>() : MyTrait, MyAbstractClass() {
    override fun foo() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass3<!>() : MyTrait, MyAbstractClass() {
    override fun bar() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass4<!>() : MyTrait, MyAbstractClass() {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo()<!> {}
    <!VIRTUAL_MEMBER_HIDDEN, MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val pr : Unit<!>
    <!NOTHING_TO_OVERRIDE!>override<!> fun other() {}
    <!NOTHING_TO_OVERRIDE!>override<!> val otherPr : Int = 1
}

class MyChildClass1() : MyClass() {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo()<!> {}
    <!VIRTUAL_MEMBER_HIDDEN!>val pr : Unit<!> = Unit
    override fun bar() {}
    override val prr : Unit = Unit
}

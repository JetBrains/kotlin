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

class MyIllegalClass : MyTrait, MyAbstractClass() {}

class MyIllegalClass2() : MyTrait, MyAbstractClass() {
    override fun foo() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

class MyIllegalClass3() : MyTrait, MyAbstractClass() {
    override fun bar() {}
    override val pr : Unit = Unit
    override val prr : Unit = Unit
}

class MyIllegalClass4() : MyTrait, MyAbstractClass() {
    fun foo() {}
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val pr : Unit<!>
    override fun other() {}
    override val otherPr : Int = 1
}

class MyChildClass1() : MyClass() {
    fun foo() {}
    val pr : Unit = Unit
    override fun bar() {}
    override val prr : Unit = Unit
}

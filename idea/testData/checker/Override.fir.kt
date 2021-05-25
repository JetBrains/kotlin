package override

    interface MyTrait {
        fun foo()
    }

    abstract class MyAbstractClass {
        abstract fun bar()
    }

    open class MyClass : MyTrait, MyAbstractClass() {
        override fun foo() {}
        override fun bar() {}
    }

    class MyChildClass : MyClass() {}

    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass is not abstract and does not implement abstract member foo">class MyIllegalClass</error> : MyTrait, MyAbstractClass() {}

    <error descr="[ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass2 is not abstract and does not implement abstract base class member bar">class MyIllegalClass2</error> : MyTrait, MyAbstractClass() {
        override fun foo() {}
    }

    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass3 is not abstract and does not implement abstract member foo">class MyIllegalClass3</error> : MyTrait, MyAbstractClass() {
        override fun bar() {}
    }

    <error descr="[ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass4 is not abstract and does not implement abstract base class member bar">class MyIllegalClass4</error> : MyTrait, MyAbstractClass() {
        fun <error descr="[VIRTUAL_MEMBER_HIDDEN] 'foo' hides member of supertype 'MyTrait' and needs 'override' modifier">foo</error>() {}
        override fun other() {}
    }

    class MyChildClass1 : MyClass() {
        fun foo() {}
        override fun bar() {}
    }

    interface MyTrait<T> {
        fun foo(t: T) : T
    }

    abstract class MyAbstractClass<T> {
        abstract fun bar(t: T) : T
    }

    open class MyGenericClass<T> : MyTrait<T>, MyAbstractClass<T>() {
        override fun foo(t: T) = t
        override fun bar(t: T) = t
    }

    class MyChildClass : MyGenericClass<Int>() {}
    class MyChildClass1<T> : MyGenericClass<T>() {}
    class MyChildClass2<T> : MyGenericClass<T>() {
        fun foo(t: T) = t
        override fun bar(t: T) = t
    }

    open class MyClass : MyTrait<Int>, MyAbstractClass<String>() {
        override fun foo(t: Int) = t
        override fun bar(t: String) = t
    }

    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalGenericClass1 is not abstract and does not implement abstract member foo">class MyIllegalGenericClass1</error><T> : MyTrait<T>, MyAbstractClass<T>() {}
    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalGenericClass2 is not abstract and does not implement abstract member foo">class MyIllegalGenericClass2</error><T, R> : MyTrait<T>, MyAbstractClass<R>() {
        override fun foo(r: R) = r
    }
    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass1 is not abstract and does not implement abstract member foo">class MyIllegalClass1</error> : MyTrait<Int>, MyAbstractClass<String>() {}

    <error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class MyIllegalClass2 is not abstract and does not implement abstract member foo">class MyIllegalClass2</error><T> : MyTrait<Int>, MyAbstractClass<Int>() {
        fun foo(t: T) = t
        fun bar(t: T) = t
    }

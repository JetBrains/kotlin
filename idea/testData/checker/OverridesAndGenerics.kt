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
        <error>fun foo(t: T)</error> = t
        override fun bar(t: T) = t
    }

    open class MyClass : MyTrait<Int>, MyAbstractClass<String>() {
        override fun foo(t: Int) = t
        override fun bar(t: String) = t
    }

    <error>class MyIllegalGenericClass1</error><T> : MyTrait<T>, MyAbstractClass<T>() {}
    <error>class MyIllegalGenericClass2</error><T, R> : MyTrait<T>, MyAbstractClass<R>() {
        <error>override</error> fun foo(r: R) = r
    }
    <error>class MyIllegalClass1</error> : MyTrait<Int>, MyAbstractClass<String>() {}

    <error>class MyIllegalClass2</error><T> : MyTrait<Int>, MyAbstractClass<Int>() {
        <error>fun foo(t: T)</error> = t
        <error>fun bar(t: T)</error> = t
    }

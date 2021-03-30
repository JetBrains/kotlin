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

    class MyIllegalGenericClass1<T> : MyTrait<T>, MyAbstractClass<T>() {}
    class MyIllegalGenericClass2<T, R> : MyTrait<T>, MyAbstractClass<R>() {
        override fun foo(r: R) = r
    }
    class MyIllegalClass1 : MyTrait<Int>, MyAbstractClass<String>() {}

    class MyIllegalClass2<T> : MyTrait<Int>, MyAbstractClass<Int>() {
        fun foo(t: T) = t
        fun bar(t: T) = t
    }

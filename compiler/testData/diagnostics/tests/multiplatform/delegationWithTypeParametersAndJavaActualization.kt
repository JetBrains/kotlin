// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>Base1<!><T> {
    fun foo(t: T): T
}

expect interface <!NO_ACTUAL_FOR_EXPECT!>Base2<!><K> {
    fun foo(t: K): K
}

class Test1<T>(val a: Base1<T>, val b: Base2<T>): Base1<T> by a, Base2<T> by b {
    override fun foo(t: T): T {
        return t
    }
}

class Test2(val a: Base1<Any?>, val b: Base2<Any?>): Base1<Any?> by a, Base2<Any?> by b {
    override fun foo(t: Any?): Any? {
        return t
    }
}

class Test3(val a: Base1<Int>, val b: Base2<Number>): Base1<Int> by a, Base2<Number> by b

class Test4(val a: Base1<Int>, val b: Base2<Any>): Base1<Int> by a, Base2<Any> by b

// MODULE: platform()()(common)
// FILE: Base1Java.java
public interface Base1Java<T> {
    T foo(T t);
}

// FILE: Base2JavaImpl.java
public class Base2JavaImpl implements Base2 {
    @Override
    public Object foo(Object o) {
        return null;
    }
}

// FILE: platform.kt
actual typealias Base1<T> = Base1Java<T>

actual interface Base2<T> {
    actual fun foo(t: T): T
}

class Base1Impl<T>: Base1<T> {
    override fun foo(t: T): T {
        return t
    }
}

class Base2Impl<T>: Base2<T> {
    override fun foo(t: T): T {
        return t
    }
}

fun test(){
    Test1(Base1Impl(), Base2Impl<Int>()).foo(1)
    Test1(Base1Impl<String>(), Base2Impl()).foo("1")
    Test1(Base1Impl(), Base2JavaImpl()).foo(1)

    Test2(Base1Impl(), Base2Impl()).foo(1)
    Test2(Base1Impl(), Base2Impl()).foo("1")
    Test2(Base1Impl(), Base2JavaImpl()).foo(1)
}
// MODULE: common
// FILE: common.kt
expect interface Base1<T>{
    fun foo(t: T): T
    val a : T
}

expect interface Base2<T>{
    fun foo(t: T): T
    val a : T
}

class Test<T>(val x : Base1<T>, val y : Base2<T>) : Base1<T> by x, Base2<T> by y {
    override fun foo(t: T): T { return t }
    override val a : T
        get() = 1 <!UNCHECKED_CAST, UNCHECKED_CAST{METADATA}!>as T<!>
}

class Test2(val x : Base1<Int>, val y : Base2<Int>): Base1<Int> by x, Base2<Int> by y {
    override fun foo(t: Int): Int {
        return t
    }

    override val a: Int
        get() = 1
}

class Test3(val x : Base1<Int>, val y : Base2<Number>): Base1<Int> by x, Base2<Number> by y {
    override val a: Int
        get() = 1

    override fun foo(t: Number): Number {
        return 1
    }
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base1<T> {
    actual fun foo(t: T): T
    actual val a : T
}

actual interface Base2<K> {
    actual fun foo(t: K): K
    actual val a : K
}

class Base1Impl<T>(override val a: T) : Base1<T> {
    override fun foo(t: T): T {
        return t
    }
}
class Base2Impl<K>(override val a: K) : Base2<K> {
    override fun foo(t: K): K {
        return t
    }
}

class Base1Impl2(override val a: Int) : Base1<Int> {
    override fun foo(t: Int): Int {
        return t
    }
}
class Base2Impl2(override val a: Number): Base2<Number> {
    override fun foo(t: Number): Number {
        return t
    }
}

class Base2Impl3(override val a: Int): Base2<Int> {
    override fun foo(t: Int): Int {
        return t
    }
}

fun test(){
    Test(x = Base1Impl(1), y = Base2Impl(2)).foo(1)
    Test(x = Base1Impl(1), y = Base2Impl(2)).a

    Test(x = Base1Impl(1), y = Base2Impl("")).foo(1)
    Test(x = Base1Impl(1), y = Base2Impl("")).foo(1.3)

    Test2(x = Base1Impl2(1), y = Base2Impl3(2)).foo(1)
    Test2(x = Base1Impl2(1), y = Base2Impl3(2)).a

    Test3(Base1Impl2(1), Base2Impl2(7.6)).foo(5.6)
    Test3(Base1Impl2(1), Base2Impl2(7.8)).foo(1)
}


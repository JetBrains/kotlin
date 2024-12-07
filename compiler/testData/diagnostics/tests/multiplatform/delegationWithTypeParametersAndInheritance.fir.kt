// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: Ignoring an implementation of the method `java.lang.Object Test2.foo(java.lang.Object)` because it has multiple definitions
// MODULE: common
// FILE: common.kt
expect interface Base1<T>{
    fun foo(a: T): T
}

expect interface Base2<T>{
    fun foo(a: T): T
}

class Test(val x: Base1<Int>) : Base2<Int>, Base1<Int> by x

class Test2(val x: Base1<String>) : Base2<Int>, Base1<String> by x {
    override fun foo(a: Int): Int {
        return a
    }
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base1<T> {
    actual fun foo(a: T): T
}

actual interface Base2<T> {
    actual fun foo(a: T): T
}

class Base1Impl<T> : Base1<T>{
    override fun foo(a: T): T {
        return a
    }
}

open class Base2Impl<T> : Base2<T>{
    override fun foo(a: T): T {
        return a
    }
}

class Test3(val x: Base1<String>) : Base2Impl<Int>(), Base1<String> by x

fun test(){
    Test(Base1Impl()).foo(1)
    Test2(Base1Impl<String>()).foo(1)
    Test2(Base1Impl<String>()).foo("a")
    Test3(Base1Impl<String>()).foo(1)
    Test3(Base1Impl<String>()).foo("a")
}




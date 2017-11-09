// !DIAGNOSTICS: -UNUSED_PARAMETER

class A1 {
    fun <T> a1(t: T): Unit {}
    fun test1(): (String) -> Unit = A1()::a1
}

class A2 {
    fun <K, V> a2(key: K): V = TODO()

    fun test1(): (String) -> Unit = A2()::a2
    fun <T3> test2(): (T3) -> T3 = A2()::a2
}

class A3<T> {
    fun <V> a3(key: T): V = TODO()

    fun test1(): (T) -> Int = this::a3
    fun test2(): (T) -> Unit = A3<T>()::a3
    fun test3(): (Int) -> String = A3<Int>()::a3

    fun <R> test4(): (R) -> Unit = <!TYPE_MISMATCH!>this::<!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>a3<!><!>
    fun <R> test5(): (T) -> R = this::a3
}
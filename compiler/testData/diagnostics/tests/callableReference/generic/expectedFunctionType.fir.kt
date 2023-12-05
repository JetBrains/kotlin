// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id(x: K) = x

class A1 {
    fun <T> a1(t: T): Unit {}
    fun test1(): (String) -> Unit = A1()::a1
    fun test2(): (String) -> Unit = id(A1()::a1)
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

    fun <R> test4(): (R) -> Unit = <!RETURN_TYPE_MISMATCH!>this::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a3<!><!>
    fun <R> test5(): (T) -> R = this::a3
}

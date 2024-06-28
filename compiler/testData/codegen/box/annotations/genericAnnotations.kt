// MODULE: lib
// FILE: l1.kt
package ann

annotation class Test1<T>(val x: Int)

annotation class Test2<T1 : Any, T2>(val x: Int = 0)

interface I<T>

annotation class Test3<T1, T2 : I<T1>>(val x: Test1<I<T2>>)

class C<T> : I<T>

annotation class Test4(val x: Array<Test3<Int, C<Int>>>)

class ARG

annotation class Test5<T>(vararg val xs: Test3<T, C<T>>)

// FILE: l2.kt

import ann.*

@Test1<ARG>(42)
@Test2<String, String>(38)
@Test3<String, C<String>>(Test1(39))
@Test4([Test3<Int, C<Int>>(Test1(40)), Test3<Int, C<Int>>(Test1(50)), Test3<Int, C<Int>>(Test1(60))])
//@Test5<ARG>(*arrayOf(Test3(Test1(70))), *arrayOf(Test3(Test1(80)))) <-- KT-45414
class O {
    fun test(): String = "O"
}

// MODULE: main(lib)
// FILE: main.kt

import ann.*

@Test1<ARG>(24)
@Test2<String, String>(83)
@Test3<String, C<String>>(Test1(93))
@Test4([Test3<Int, C<Int>>(Test1(44)), Test3<Int, C<Int>>(Test1(55)), Test3<Int, C<Int>>(Test1(66))])
//@Test5<ARG>(*arrayOf(Test3(Test1(77))), *arrayOf(Test3(Test1(88)))) <-- KT-45414
class K {
    fun test(): String = "K"
}

fun box(): String = O().test() + K().test()

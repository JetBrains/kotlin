// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-60136

package ann

annotation class Test1<T>(val x: Int)

annotation class Test2<T1 : Any, T2>(val x: Int = 0)

interface I<T>

annotation class Test3<T1, T2 : I<T1>>(val x: Test1<I<T2>>)

class C<T> : I<T>

annotation class Test4(val x: Array<Test3<Int, C<Int>>>)

class ARG

annotation class Test5<T>(vararg val xs: Test3<T, C<T>>)


@Test1<ARG>(42)
@Test2<String, String>(38)
@Test3<String, C<String>>(Test1(39))
@Test4([Test3(Test1(40)), Test3(Test1(50)), Test3(Test1(60))])
@Test5<ARG>(*arrayOf(Test3(Test1(70))), *arrayOf(Test3(Test1(80))))
class CC

package redundantSamConstructor

import a.*

fun test() {
    MyJavaClass.foo1(Runnable { }, 1)
    MyJavaClass.foo1(Runnable { }, Runnable { })
    MyJavaClass.foo2(1, Runnable { })
    MyJavaClass.foo2(Runnable { }, Runnable { })

    A<String>().foo(JFunction1<String> {})
}
package redundantSamConstructor

import a.*

fun testNonApplicableAmbiguity(p: Int) {
    GenericClassCantBeReplaced.staticFun1(JFunction0 { "" })
    GenericClassCantBeReplaced.staticFun2(JFunction0 { "" }, JFunction0 { "" })

    val klass = GenericClassCantBeReplaced()
    klass.memberFun1(JFunction0 { "" })
    klass.memberFun2(JFunction0 { "" }, JFunction0 { "" })

    MyJavaClass.staticFun1(Runnable {
        if (p > 0) return@Runnable
        print(1)
    })

    MyJavaClass.staticFun2(Runnable { }, Runnable {
        if (p > 0) return@Runnable
        print(1)
    })
}
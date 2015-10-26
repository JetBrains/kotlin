package redundantSamConstructor

import a.*

fun test() {
    val runnable = Runnable { }
    val klass = MyJavaClass()

    MyJavaClass.staticFun1(Runnable { })
    MyJavaClass.staticFun1(runnable)
    MyJavaClass.staticFun2(Runnable { }, Runnable { })
    MyJavaClass.staticFun2(runnable, Runnable { })
    MyJavaClass.staticFun2({ }, { })
    MyJavaClass.staticFun2(
            object: Runnable {
                override fun run() { }
            },
            object: Runnable {
                override fun run() { }
            })

    MyJavaClass.staticFunWithOtherParam(1, Runnable { })
    MyJavaClass.staticFunWithOtherParam(1, runnable)

    klass.memberFun1(Runnable { })
    klass.memberFun1(runnable)
    klass.memberFun2(Runnable { }, Runnable { })
    klass.memberFun2(runnable, Runnable { })
    klass.memberFun2({ }, { })
    klass.memberFun2(
            object: Runnable {
                override fun run() { }
            },
            object: Runnable {
                override fun run() { }
            })

    klass.memberFunWithOtherParam(1, Runnable { })
    klass.memberFunWithOtherParam(1, runnable)

    MyJavaClass.staticFunWithOtherParam(1, java.lang.Runnable { })
    klass.memberFun1(java.lang.Runnable { })
}
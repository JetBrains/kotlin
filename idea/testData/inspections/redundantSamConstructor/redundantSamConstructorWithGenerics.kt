package redundantSamConstructor

import a.*

fun testGenerics() {
    val runnable = JFunction1<String> { }
    val klass = GenericClass1()

    GenericClass1.staticFun1(JFunction1<String> { })
    GenericClass1.staticFun1(runnable)
    GenericClass1.staticFun2(JFunction1<String> { }, JFunction1<String> { })
    GenericClass1.staticFun2(runnable, JFunction1<String> { })
    GenericClass1.staticFun2({ s: String ->  }, { s: String -> })

    GenericClass1.staticFunWithOtherParam(1, JFunction1<String> { })
    GenericClass1.staticFunWithOtherParam(1, runnable)

    klass.memberFun1(JFunction1<String> { })
    klass.memberFun1(runnable)
    klass.memberFun2(JFunction1<String> { }, JFunction1<String> { })
    klass.memberFun2(runnable, JFunction1<String> { })
    klass.memberFun2({ s: String ->  }, { s: String -> })

    klass.memberFunWithOtherParam(1, JFunction1<String> { })
    klass.memberFunWithOtherParam(1, runnable)
}
package redundantSamConstructor

import a.*

fun testNonApplicableAmbiguity() {
    GenericClassCantBeReplaced.staticFun1(JFunction0 { "" })
    GenericClassCantBeReplaced.staticFun2(JFunction0 { "" }, JFunction0 { "" })

    val klass = GenericClassCantBeReplaced()
    klass.memberFun1(JFunction0 { "" })
    klass.memberFun2(JFunction0 { "" }, JFunction0 { "" })
}
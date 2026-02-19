// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
open class Super {
    val superProp: String = ""
    fun superFunc() {}
}

open class MyClass {
    object InheritorObject: MyClass() {
        val prop: String = ""
    }

    class InheritorClass: MyClass() {
        val prop: String = ""
    }

    object NestedObject

    companion object: Super() {
        val propMyClass: MyClass = TODO()
        val propInheritorClass: InheritorClass = InheritorClass()
        val propInheritorObject: InheritorObject = InheritorObject
        val prop: String = ""
        const val constProp: String = ""
        val lazyProp: String by lazy {""}
        fun func(): MyClass = TODO()
        val lambdaProp = {InheritorObject}
    }
}

fun <T>receiveParametrizedLambda(l: (T)->Unit) { }

fun testReceiveParametrizedLambda() {
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == InheritorObject }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == propMyClass }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == propInheritorClass }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == propInheritorObject }

    receiveParametrizedLambda<MyClass> { i: MyClass -> i == prop }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == superProp }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == constProp }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == lazyProp }

    receiveParametrizedLambda<MyClass> { i: MyClass -> i == NestedObject }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == lambdaProp }

    receiveParametrizedLambda<MyClass> { i: MyClass -> i == func() }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == superFunc() }
    receiveParametrizedLambda<MyClass> { i: MyClass -> i == InheritorClass() }
}

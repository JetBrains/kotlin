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
fun <T>receiveLambda(l: () -> T) {}

fun testReceivedLambda() {
    receiveLambda<MyClass> { InheritorObject }
    receiveLambda<MyClass> { propMyClass }
    receiveLambda<MyClass> { propInheritorClass }
    receiveLambda<MyClass> { propInheritorObject }

    receiveLambda<MyClass> { prop }
    receiveLambda<MyClass> { superProp }
    receiveLambda<MyClass> { constProp }
    receiveLambda<MyClass> { lambdaProp }
    receiveLambda<MyClass> { lazyProp }
    receiveLambda<MyClass> { NestedObject}

    receiveLambda<MyClass> { func() }
    receiveLambda<MyClass> { InheritorClass() }
    receiveLambda<MyClass> { superFunc() }
}

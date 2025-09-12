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
        val lambdaProp = {InheritorObject}
        fun func(): MyClass = TODO()
    }
}

fun testLambda(lMyClass: (arg: MyClass) -> Unit) {
    lMyClass(InheritorObject)
    lMyClass(propMyClass)
    lMyClass(propInheritorClass)
    lMyClass(propInheritorObject)

    lMyClass(NestedObject)
    lMyClass(prop)
    lMyClass(superProp)
    lMyClass(constProp)
    lMyClass(lazyProp)
    lMyClass(lambdaProp)

    lMyClass(InheritorClass())
    lMyClass(func())
    lMyClass(superFunc())
}

// WITH_STDLIB
// ISSUE: KT-75315
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

fun <T>receiverVarArg(vararg arg: T) = arg

fun testVarArg(i: MyClass.InheritorClass) {
    receiverVarArg<MyClass>(InheritorObject)
    receiverVarArg<MyClass>(i)
    receiverVarArg<MyClass>(propMyClass)
    receiverVarArg<MyClass>(propInheritorClass)
    receiverVarArg<MyClass>(propInheritorObject)

    receiverVarArg<MyClass>(prop)
    receiverVarArg<MyClass>(constProp)
    receiverVarArg<MyClass>(lazyProp)
    receiverVarArg<MyClass>(lambdaProp)
    receiverVarArg<MyClass>(superProp)

    receiverVarArg<MyClass>(func())
    receiverVarArg<MyClass>(superFunc())
}

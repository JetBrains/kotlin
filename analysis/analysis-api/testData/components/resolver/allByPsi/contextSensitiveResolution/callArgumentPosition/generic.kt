// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MySuper {
    val superProp: String = ""
}

open class MyClass {
    object InheritorObject: MyClass() {}

    class InheritorClass: MyClass() {}

    object NestedObject

    companion object: MySuper() {
        val propMyClass: MyClass = TODO()
        val propInheritorClass: InheritorClass = InheritorClass()
        val propInheritorObject: InheritorObject = InheritorObject
        val prop: String = ""
        fun func(): MyClass = TODO()
        val lazyProp: String by lazy {""}
        val lambdaProp = {InheritorObject}
    }
}

fun consumeMyClass(arg: MyClass) {}

fun <T> test(arg: T): T = arg

fun testGeneric() {
    consumeMyClass(test(InheritorObject))

    consumeMyClass(test(propMyClass))
    consumeMyClass(test(propInheritorClass))
    consumeMyClass(test(propInheritorObject))

    consumeMyClass(test(NestedObject))
    consumeMyClass(test(prop))
    consumeMyClass(test(superProp))
    consumeMyClass(test(lazyProp))
    consumeMyClass(test(lambdaProp))

    consumeMyClass(test(InheritorClass()))
    consumeMyClass(test(func()))
}

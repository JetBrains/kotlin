fun function(): String = "FAIL: function"
fun removedFunction(): String = "FAIL: removedFunction"
val property: String get() = "FAIL: property"
val removedProperty: String get() = "FAIL: removedProperty"

class A {
    fun function(): String = "FAIL: A.function"
    fun removedFunction(): String = "FAIL: A.removedFunction"
    val property1: String get() = "FAIL: property1"
    val removedProperty1: String get() = "FAIL: removedProperty1"
    val property2: String = "FAIL: property2"
    val removedProperty2: String = "FAIL: removedProperty2"
}

open class C {
    open fun removedOpenFunction(): String = "FAIL: C.removedOpenFunction"
    open val removedOpenProperty: String get() = "FAIL: C.removedOpenProperty"
}

interface I {
    fun removedOpenFunction(): String = "FAIL: I.removedOpenFunction"
    val removedOpenProperty: String get() = "FAIL: I.removedOpenProperty"
}

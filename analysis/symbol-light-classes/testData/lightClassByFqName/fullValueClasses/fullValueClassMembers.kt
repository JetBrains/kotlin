// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

class OriginalClass

value class ValueClass(val original: OriginalClass, val count: Int) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}
    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this
    var propertyWithAccessors: Int
        get() = 1
        set(value) {}

    object RegularObject {}

    inner class Inner(val nested: Int)

    companion object {
        val companionPropertyWithValueClassType: ValueClass? = null
        val companionProperty: Int = 0

        fun companionFunction() {}
        fun companionFunctionWithValueClassType(): ValueClass? = null
    }
}

// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

class OriginalClass

@JvmInline
value class ValueClass(val original: OriginalClass) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}
    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this

    object RegularObject {}

    companion object {
        val companionPropertyWithValueClassType: ValueClass? = null
        val companionProperty: Int = 0

        fun companionFunction() {}
        fun companionFunctionWithValueClassType(): ValueClass? = null
    }
}

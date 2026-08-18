// WITH_STDLIB
// TARGET_PLATFORM: JVM

class OriginalClass

@JvmInline
value class ValueClass(val original: OriginalClass) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}

    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this

    override fun toString(): String = "ValueClass"

    companion object {
        val companionProperty: Int = 0
        val companionPropertyWithValueClassType: ValueClass? = null

        fun companionFunction() {}
        fun companionFunctionWithValueClassType(): ValueClass? = null
    }
}

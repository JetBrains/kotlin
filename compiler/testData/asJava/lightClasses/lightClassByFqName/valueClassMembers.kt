// pack.ValueClass
// WITH_STDLIB
package pack

class OriginalClass

@JvmInline
value class ValueClass(val original: OriginalClass) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}
    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this
}

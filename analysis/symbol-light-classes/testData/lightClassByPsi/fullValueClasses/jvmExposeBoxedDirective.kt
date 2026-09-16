// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// JVM_EXPOSE_BOXED

package pack

value class ValueClass(val first: String, val second: Int) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}
    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this

    companion object {
        val companionPropertyWithValueClassType: ValueClass? = null
        fun companionFunctionWithValueClassType(): ValueClass? = null
    }
}

class Regular(val value: ValueClass) {
    fun function(v: ValueClass): ValueClass = v
    var property: ValueClass? = null
}

data class DataClass(val value: ValueClass)

fun topLevelFunction(v: ValueClass): ValueClass = v
var topLevelProperty: ValueClass? = null

// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

abstract value class AbstractValueClass(parameter: Int) {
    abstract val abstractProperty: Int
    open val openProperty: Int get() = 1
    val finalProperty: Int get() = 2

    abstract fun abstractFunction()
    open fun openFunction() {}
    fun finalFunction() {}
}

sealed value class SealedValueClass(parameter: Int) : AbstractValueClass(parameter) {
    abstract val code: Int
}

value class FinalValueClass(val first: Int, val second: Int) : SealedValueClass(first) {
    override val abstractProperty: Int get() = first
    override val code: Int get() = second

    override fun abstractFunction() {}
    override fun openFunction() {}
}

value object ValueObject : SealedValueClass(0) {
    override val abstractProperty: Int get() = 0
    override val code: Int get() = 0

    override fun abstractFunction() {}
}

open class OpenRegularClass(override val abstractProperty: Int, var mutable: String) : SealedValueClass(abstractProperty) {
    override val code: Int get() = 0

    override fun abstractFunction() {}
}

class FinalRegularClass(override val abstractProperty: Int) : SealedValueClass(abstractProperty) {
    override val code: Int get() = 0

    override fun abstractFunction() {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: AbstractValueClass.class[equals;hashCode;toString], SealedValueClass.class[equals;hashCode;toString]

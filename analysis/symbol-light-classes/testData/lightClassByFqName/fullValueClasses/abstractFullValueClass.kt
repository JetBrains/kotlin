// pack.AbstractValueClass
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
    fun functionWithSelfParameter(v: AbstractValueClass): AbstractValueClass = v
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: AbstractValueClass.class[equals;hashCode;toString]

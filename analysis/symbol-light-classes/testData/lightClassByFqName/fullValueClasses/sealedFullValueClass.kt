// pack.SealedValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

sealed value class SealedValueClass {
    abstract val code: Int

    fun isSuccess(): Boolean = code == 0
}

value class Success(val payload: String, val size: Int) : SealedValueClass() {
    override val code: Int get() = 0
}

value object Empty : SealedValueClass() {
    override val code: Int get() = 1
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: SealedValueClass.class[equals;hashCode;toString]

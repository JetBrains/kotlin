// pack.ValueObject
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

sealed value class SealedValueClass {
    abstract val code: Int
}

value object ValueObject : SealedValueClass() {
    override val code: Int get() = 0

    val property: Int get() = 42

    const val constant: Int = 43

    fun function(): String = "function"
    fun functionWithSelfParameter(v: ValueObject): ValueObject = v
}

// LIGHT_ELEMENTS_NO_DECLARATION: ValueObject.class[INSTANCE;ValueObject]

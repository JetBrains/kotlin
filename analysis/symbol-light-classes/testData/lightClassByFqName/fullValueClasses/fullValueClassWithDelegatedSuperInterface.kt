// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package pack

interface Interface {
    fun regularFunction() {}
    var regularVariable: Int

    fun functionWithValueParam(v: ValueClassImpl)
    val propertyWithValueClass: ValueClassImpl
}

value class ValueClassImpl(val int: Int, val long: Long) : Interface {
    override fun regularFunction() {}

    override var regularVariable: Int
        get() = 0
        set(value) {}

    override fun functionWithValueParam(v: ValueClassImpl) {}

    override val propertyWithValueClass: ValueClassImpl get() = this

    override fun toString(): String = "ValueClass"
}

value class ValueClass(val value: ValueClassImpl, val other: ValueClassImpl) : Interface by value

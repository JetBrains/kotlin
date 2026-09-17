// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package pack

interface Interface {
    fun regularFunction() {}
    var regularVariable: Int

    fun functionWithValueParam(v: ValueClass)
    val propertyWithValueClass: ValueClass
}

value class ValueClass(val int: Int, val long: Long) : Interface {
    override fun regularFunction() {}

    override var regularVariable: Int
        get() = 0
        set(value) {}

    override fun functionWithValueParam(v: ValueClass) {}

    override val propertyWithValueClass: ValueClass get() = this

    override fun toString(): String = "ValueClass"
}

// pack.ValueClass
// WITH_STDLIB
package pack

interface Interface {
    fun regularFunction() {}
    var regularVariable: Int

    fun functionWithValueParam(v: ValueClass)
    val propertyWithValueClass: ValueClass
}

@JvmInline
value class ValueClass(val int: Int) : Interface {
    override fun regularFunction() {}

    override var regularVariable: Int
        get() = 0
        set(value) {}

    override fun functionWithValueParam(v: ValueClass) {}

    override val propertyWithValueClass: ValueClass get() = this

    override fun toString(): String = "ValueClass"
}

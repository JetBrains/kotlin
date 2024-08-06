// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

interface Interface {
    fun regularFunction() {}
    var regularVariable: Int

    fun functionWithValueParam(v: ValueClassImpl)
    val propertyWithValueClass: ValueClassImpl
}

@JvmInline
value class ValueClassImpl(val int: Int) : Interface {
    override fun regularFunction() {}

    override var regularVariable: Int
        get() = 0
        set(value) {}

    override fun functionWithValueParam(v: ValueClassImpl) {}

    override val propertyWithValueClass: ValueClassImpl get() = this

    override fun toString(): String = "ValueClass"
}


@JvmInline
value class ValueClass(val value: ValueClassImpl) : Interface by value

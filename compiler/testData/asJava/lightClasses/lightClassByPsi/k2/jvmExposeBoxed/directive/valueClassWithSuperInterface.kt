// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

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

// DECLARATIONS_NO_LIGHT_ELEMENTS: Interface.class[functionWithValueParam;propertyWithValueClass]
// LIGHT_ELEMENTS_NO_DECLARATION: Interface.class[functionWithValueParam-0JCZ7rA;getPropertyWithValueClass-wCez43g], ValueClass.class[constructor-impl;equals-impl;equals-impl0;functionWithValueParam-0JCZ7rA;getPropertyWithValueClass-wCez43g;getRegularVariable-impl;hashCode-impl;regularFunction-impl;setRegularVariable-impl;toString-impl]
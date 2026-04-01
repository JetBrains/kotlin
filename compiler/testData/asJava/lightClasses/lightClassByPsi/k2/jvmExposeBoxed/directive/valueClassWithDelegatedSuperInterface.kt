// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

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

// DECLARATIONS_NO_LIGHT_ELEMENTS: Interface.class[functionWithValueParam;propertyWithValueClass]
// LIGHT_ELEMENTS_NO_DECLARATION: Interface.class[functionWithValueParam-_dSbK5w;getPropertyWithValueClass-NoPZT8w], ValueClass.class[constructor-impl;equals-impl;equals-impl0;functionWithValueParam-_dSbK5w;getPropertyWithValueClass-NoPZT8w;getRegularVariable-impl;getValue-NoPZT8w;hashCode-impl;regularFunction-impl;setRegularVariable-impl;toString-impl], ValueClassImpl.class[constructor-impl;equals-impl;equals-impl0;functionWithValueParam-_dSbK5w;getPropertyWithValueClass-NoPZT8w;getRegularVariable-impl;hashCode-impl;regularFunction-impl;setRegularVariable-impl;toString-impl]
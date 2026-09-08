// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class ValueInt(val i: Int)

interface Iface {
    fun useValueInt(valueInt: ValueInt)
    fun returnValueInt(): ValueInt
    var valueInt: ValueInt
}

class Clazz : Iface {
    override fun useValueInt(valueInt: ValueInt) {}

    override fun returnValueInt(): ValueInt = ValueInt(42)

    override var valueInt: ValueInt = ValueInt(42)
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Iface.class[returnValueInt;useValueInt;valueInt]
// LIGHT_ELEMENTS_NO_DECLARATION: Clazz.class[getValueInt-EIAok-E;returnValueInt-EIAok-E;setValueInt-1UXusoc;useValueInt-1UXusoc], Iface.class[getValueInt-EIAok-E;returnValueInt-EIAok-E;setValueInt-1UXusoc;useValueInt-1UXusoc], ValueInt.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

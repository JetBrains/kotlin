// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class ValueInt(val i: Int) {
    internal fun internalMember() {}
}

internal fun internalTopLevel(value: ValueInt): ValueInt = value

internal var internalTopLevelProperty: ValueInt
    get() = ValueInt(0)
    set(value) {}

// LIGHT_ELEMENTS_NO_DECLARATION: InternalDeclarationsKt.class[internalTopLevel-1UXusoc;setInternalTopLevelProperty-1UXusoc], ValueInt.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;internalMember-impl$main;toString-impl]

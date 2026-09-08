// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class ValueInt(val i: Int)

enum class Enum {
    FIRST {
        override fun overridableFunction(valueInt: ValueInt) {}
    },
    SECOND;

    fun finalFunction(valueInt: ValueInt) {}

    open fun overridableFunction(valueInt: ValueInt) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Enum.class[overridableFunction]
// LIGHT_ELEMENTS_NO_DECLARATION: Enum.class[finalFunction-1UXusoc;getEntries;overridableFunction-1UXusoc;valueOf;values], ValueInt.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

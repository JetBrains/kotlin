// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class C(val value: Int) {
    fun member(): Int = value

    companion {
        fun fromInt(value: Int): C = C(value)
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: C.class[fromInt;member]
// LIGHT_ELEMENTS_NO_DECLARATION: C.class[constructor-impl;equals-impl;equals-impl0;fromInt-Aykwt80;hashCode-impl;member-impl;toString-impl]

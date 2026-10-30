// one.V
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class V(val x: Int) {
    companion {
        val x: String = "x"
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: V.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

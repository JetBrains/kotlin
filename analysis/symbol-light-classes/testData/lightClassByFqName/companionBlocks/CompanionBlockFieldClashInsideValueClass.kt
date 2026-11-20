// one.V
// LANGUAGE: +CompanionBlocks
package one

inline value class V(val x: Int) {
    companion {
        val x: String = "x"
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: V.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

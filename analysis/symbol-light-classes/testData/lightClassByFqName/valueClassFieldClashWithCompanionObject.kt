// one.A
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class A(val value: String) {
    companion object {
        val value = "companion"
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: A.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

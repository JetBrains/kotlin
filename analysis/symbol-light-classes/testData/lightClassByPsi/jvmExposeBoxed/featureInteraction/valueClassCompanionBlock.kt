// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +CompanionBlocks +CompanionExtensions
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
@JvmInline
value class V(val value: String) {
    companion {
        fun plain(): Int = 1
        val label: String get() = "label"

        fun of(value: String): V = V(value)
        fun take(v: V): Int = 1
        val default: V get() = V("")

        @JvmExposeBoxed("named")
        fun explicitlyNamed(v: V): Int = 1
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: V.class[explicitlyNamed]
// LIGHT_ELEMENTS_NO_DECLARATION: V.class[constructor-impl;equals-impl;equals-impl0;explicitlyNamed-Wv3fJ18;getDefault-ONrkMxo;hashCode-impl;named;of-vEZgED0;take-Wv3fJ18;toString-impl]

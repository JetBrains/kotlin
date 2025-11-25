// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) {
    companion object {
        fun unwrap(s: StringWrapper): String = s.s
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: StringWrapper.class[unwrap]
// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[Companion;constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl;unwrap-JELJCFg]
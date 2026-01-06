// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) {
    init {
        result = s
    }
}

var result = "FAIL"

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
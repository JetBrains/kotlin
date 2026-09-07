// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class JvmMarker(val value: Int)

class ExposedResult {
    fun consume(result: Result<String>?) {}

    fun <T : Result<String>> consume1(result: T?) {}

    fun <T : Result<String>?> consume2(result: T) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: ExposedResult.class[consume1;consume2]
// LIGHT_ELEMENTS_NO_DECLARATION: ExposedResult.class[consume1;consume2], JvmMarker.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

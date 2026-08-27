// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class ExposedResult {
    fun consume(result: Result<String>?) {}

    @JvmExposeBoxed("consumeSafe")
    fun consumeRename(result: Result<String>?) {}

    fun <T : Result<String>?> consume1(result: T) {}

    @JvmExposeBoxed("consumeSafe1")
    fun <T : Result<String>?> consumeRename1(result: T) {}

    fun <T : Result<String>> consume2(result: T?) {}

    @JvmExposeBoxed("consumeSafe2")
    fun <T : Result<String>> consumeRename2(result: T?) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: ExposedResult.class[consume1;consume2]
// LIGHT_ELEMENTS_NO_DECLARATION: ExposedResult.class[consume1;consume2;consumeSafe;consumeSafe1;consumeSafe2]

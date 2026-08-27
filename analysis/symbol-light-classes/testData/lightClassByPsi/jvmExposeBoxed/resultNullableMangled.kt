// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// A nullable 'kotlin.Result' on its own makes the regular and the boxed methods share one JVM signature, but any other
// value class in a parameter position mangles the name of the regular method, and a non-nullable 'kotlin.Result' is
// mapped to 'java/lang/Object' there, so in those cases both methods are generated.

@JvmInline
value class IntWrapper(val i: Int)

@JvmExposeBoxed
class ExposedResult {
    fun onlyNullableResult(r: Result<String>?) {}

    fun withNotNullResult(r: Result<String>?, o: Result<String>) {}

    fun <T : Result<String>> withNotNullTypeParameter(r: Result<String>?, t: T) {}

    fun withMangledParameter(r: Result<String>?, w: IntWrapper) {}

    fun IntWrapper.withMangledReceiver(r: Result<String>?) {}
}

// LIGHT_ELEMENTS_NO_DECLARATION: ExposedResult.class[withMangledParameter-f-kJWs0;withMangledReceiver-OH4AeLI;withNotNullTypeParameter], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

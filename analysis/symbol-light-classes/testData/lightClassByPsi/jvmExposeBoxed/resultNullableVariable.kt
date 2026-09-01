// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

// A nullable 'kotlin.Result' in a parameter position is mapped to 'kotlin/Result' in both modes and doesn't mangle a name,
// so the regular and the boxed accessors would share one JVM signature, and only the boxed one is generated.
// A getter has no value parameter, so only a receiver or a context parameter can make it clash,
// while a setter takes the type of the property as its value parameter.

@JvmExposeBoxed
class ExposedResult {
    var resultProp: Result<Int>? = null

    var Result<Int>?.resultInExtension: Int
        get() = 0
        set(value) {}

    context(r: Result<Int>?)
    var resultInContext: Int
        get() = 0
        set(value) {}

    // A renamed accessor keeps both declarations apart
    @get:JvmExposeBoxed("getRenamed")
    @set:JvmExposeBoxed("setRenamed")
    var renamedProp: Result<Int>? = null
}

// LIGHT_ELEMENTS_NO_DECLARATION: ExposedResult.class[getRenamed;getRenamedProp-xLWZpok;getResultProp-xLWZpok;setRenamed]

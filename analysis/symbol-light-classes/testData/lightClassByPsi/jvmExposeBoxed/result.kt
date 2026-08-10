// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

// 'kotlin.Result' in a parameter position doesn't mangle a name, but a boxed method is still exposed for it,
// so both methods are generated

@JvmExposeBoxed
class Exposed {
    var resultProp: Result<Int> = Result.success(1)

    fun resultInReturn(): Result<Int> = Result.success(1)
    fun resultInParameter(r: Result<Int>) {}
    fun Result<Int>.resultInExtension() {}

    context(r: Result<Int>)
    fun resultInContext() {}
}

// LIGHT_ELEMENTS_NO_DECLARATION: Exposed.class[getResultProp-d1pmJ48;resultInReturn-d1pmJ48;setResultProp]

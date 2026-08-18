// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
// JVM_EXPOSE_BOXED

// 'kotlin.Result' in a parameter position doesn't mangle a name, but a boxed method is still exposed for it,
// so both methods are generated

@JvmInline
value class Some(val value: String)

class Regular {
    var resultProp: Result<Int> = Result.success(1)

    fun resultInReturn(): Result<Int> = Result.success(1)
    fun resultInParameter(r: Result<Int>) {}
    fun Result<Int>.resultInExtension() {}

    context(r: Result<Int>)
    fun resultInContext() {}

    // A regular value class in a parameter still mangles the name, so the boxed method doesn't clash with it
    fun resultAndValueClassInParameter(r: Result<Int>, s: Some) {}

    // A boxed method is not exposed for a suspend function
    suspend fun suspendResultInParameter(r: Result<Int>) {}
}

// A boxed method is not exposed for an overridable function
open class OpenClass {
    open fun resultInParameter(r: Result<Int>) {}
}

interface RegularInterface {
    fun resultInParameter(r: Result<Int>)
}

fun topLevelResultInParameter(r: Result<Int>) {}

// DECLARATIONS_NO_LIGHT_ELEMENTS: RegularInterface.class[resultInParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: Regular.class[getResultProp-d1pmJ48;resultAndValueClassInParameter-NpkG7VQ;resultInReturn-d1pmJ48;setResultProp], RegularInterface.class[resultInParameter], Some.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]

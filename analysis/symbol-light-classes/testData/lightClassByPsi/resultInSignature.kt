// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters

// 'kotlin.Result' is a value class, but the JVM backend excludes it from parameter positions when it mangles a name,
// so only a return type is affected by it

@JvmInline
value class Some(val value: String)

var topLevelResultProp: Result<Int> = Result.success(1)

fun topLevelResultInReturn(): Result<Int> = Result.success(1)
fun topLevelResultInParameter(r: Result<Int>) {}
fun Result<Int>.topLevelResultInExtension() {}

context(r: Result<Int>)
fun topLevelResultInContext() {}

class RegularClass {
    // The type of a property is the parameter type of its setter, so only 'getClassResultProp' has a mangled name
    var classResultProp: Result<Int> = Result.success(1)
    var classNullableResultProp: Result<Int>? = null
    var Result<Int>.classPropInResultExtension: Int
        get() = 1
        set(value) {}

    fun classResultInReturn(): Result<Int> = Result.success(1)
    fun classNullableResultInReturn(): Result<Int>? = null
    fun classResultInParameter(r: Result<Int>) {}
    fun Result<Int>.classResultInExtension() {}

    context(r: Result<Int>)
    fun classResultInContext() {}

    suspend fun classSuspendResultInReturn(): Result<Int> = Result.success(1)

    @JvmName("resultInReturnWithJvmName")
    fun classResultInReturnWithJvmName(): Result<Int> = Result.success(1)

    // A regular value class in a parameter still mangles the name
    fun classResultAndValueClassInParameter(r: Result<Int>, s: Some) {}
}

interface RegularInterface {
    var interfaceResultProp: Result<Int>

    fun interfaceResultInReturn(): Result<Int>
    fun interfaceResultInParameter(r: Result<Int>)
}

// A constructor cannot be renamed, so the JVM backend makes it private instead of mangling its name,
// which doesn't happen because of 'kotlin.Result'
class ClassWithResultConstructor(r: Result<Int>)

class ClassWithValueClassConstructor(s: Some)

@JvmInline
value class ValueClassWithResult(val r: Result<Int>) {
    fun funInValueClass() {}
    fun funWithResultParameter(other: Result<Int>) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: RegularClass.class[classNullableResultInReturn;classResultAndValueClassInParameter;classResultInReturn;classSuspendResultInReturn], RegularInterface.class[interfaceResultInParameter;interfaceResultInReturn;interfaceResultProp], ValueClassWithResult.class[funInValueClass;funWithResultParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: RegularClass.class[classNullableResultInReturn-xLWZpok;classResultAndValueClassInParameter-NpkG7VQ;classResultInReturn-d1pmJ48;classSuspendResultInReturn-IoAF18A;getClassNullableResultProp-xLWZpok;getClassResultProp-d1pmJ48;setClassResultProp], RegularInterface.class[getInterfaceResultProp-d1pmJ48;interfaceResultInParameter;interfaceResultInReturn-d1pmJ48;setInterfaceResultProp], ResultInSignatureKt.class[setTopLevelResultProp], Some.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], ValueClassWithResult.class[constructor-impl;equals-impl;equals-impl0;funInValueClass-impl;funWithResultParameter-impl;getR-d1pmJ48;hashCode-impl;toString-impl]

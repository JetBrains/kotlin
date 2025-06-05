// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalVersionOverloading::class)

sealed class SealedClass{
    fun <!INVALID_VERSIONING_ON_NONFINAL_CLASS!>foo<!>(a: String = "", @IntroducedAt("1") x: Int = 1) { }
}

annotation class Annotation<!INVALID_VERSIONING_ON_ANNOTATION_CLASS!>(val s: String = "", @IntroducedAt("1") val x: Int = 0)<!>

fun outer() {
    fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>local<!>(x: Int, @IntroducedAt("1") y: String = "") {}
}

context(<!INVALID_VERSIONING_ON_RECEIVER_OR_CONTEXT_PARAMETER_POSITION!>@IntroducedAt("1")<!> x: Int)
fun withContext(a: String = "") {}

fun (<!WRONG_ANNOTATION_TARGET!>@IntroducedAt("1")<!> Int).withReceiver(a: String = "") {}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:IntroducedAt("1")<!> fun Int.withReceiverAgain(a: String = "") {}

fun withVararg1(x: String, <!INVALID_VERSIONING_ON_VARARG!>@IntroducedAt("1")<!> vararg rest: String) { }

fun withVararg2(@IntroducedAt("1") x: String = "hello", <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>vararg rest: String<!>) { }

fun withVararg3(vararg rest: String, @IntroducedAt("1") x: String = "hello") { }

@JvmInline value class ValueClass(<!INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER!>@IntroducedAt("1")<!> val n: Int = 0)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, classDeclaration, classReference,
funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, integerLiteral, localFunction,
primaryConstructor, propertyDeclaration, sealed, stringLiteral, value, vararg */

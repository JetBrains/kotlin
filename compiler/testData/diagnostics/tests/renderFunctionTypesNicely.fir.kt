// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74461
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -REDUNDANT_PROJECTION

fun <T> main() {
    val a: (() -> Int) -> String <!INITIALIZER_TYPE_MISMATCH("(() -> Int) -> String; Int")!>=<!> 10
    val b: Int.(String, Boolean) -> String <!INITIALIZER_TYPE_MISMATCH("Int.(String, Boolean) -> String; Int")!>=<!> 10
    val c: context(Int) Int.(String) -> String <!INITIALIZER_TYPE_MISMATCH("context(Int) Int.(String) -> String; Int")!>=<!> 10
    val d: suspend Int.(String) -> String <!INITIALIZER_TYPE_MISMATCH("suspend Int.(String) -> String; Int")!>=<!> 10
    val e: (() -> Int).() -> String <!INITIALIZER_TYPE_MISMATCH("(() -> Int).() -> String; Int")!>=<!> 10
    val f: (() -> Int)?.() -> String <!INITIALIZER_TYPE_MISMATCH("(() -> Int)?.() -> String; Int")!>=<!> 10
    val g: (T & Any).() -> String <!INITIALIZER_TYPE_MISMATCH("(T (of fun <T> main) & Any).() -> String; Int")!>=<!> 10
    val h: T.() -> String <!INITIALIZER_TYPE_MISMATCH("(T (of fun <T> main)).() -> String; Int")!>=<!> 10
    val i: @ExtensionFunctionType kotlin.Function1<in Number, String> <!INITIALIZER_TYPE_MISMATCH("(in Number).() -> String; Int")!>=<!> 10
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration */

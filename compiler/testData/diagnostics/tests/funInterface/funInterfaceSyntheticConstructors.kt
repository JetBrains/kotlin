// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface KRunnable {
    fun invoke()
}

typealias KRunnableAlias = KRunnable

fun foo(f: KRunnable) {}

fun test() {
    foo(KRunnable {})
    foo(KRunnableAlias {})
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, typeAliasDeclaration */

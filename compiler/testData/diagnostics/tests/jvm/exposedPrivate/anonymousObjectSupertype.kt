// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

private open class C {
    val ok: String = "OK"
}

private inline fun privateFun() = object : C() {
    fun foo() = super.ok
}.foo()

internal inline fun test() = privateFun()

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inline, propertyDeclaration,
stringLiteral, superExpression */

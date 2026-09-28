// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun Int.extFun() {}

val a: Any
    field: Int = 1

fun foo() {
    a.inc().extFun()
}

/* GENERATED_FIR_TAGS: explicitBackingField, funWithExtensionReceiver, functionDeclaration, inline, integerLiteral,
propertyDeclaration, smartcast */

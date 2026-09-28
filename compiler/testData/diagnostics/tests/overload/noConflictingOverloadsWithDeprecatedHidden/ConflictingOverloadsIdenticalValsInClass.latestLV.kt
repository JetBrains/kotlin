// RUN_PIPELINE_TILL: CODEGEN
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
class Aaa() {
    val a = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
class Aaa() {
    <!CONFLICTING_JVM_DECLARATIONS!>val a<!> = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_JVM_DECLARATIONS!>val a<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */

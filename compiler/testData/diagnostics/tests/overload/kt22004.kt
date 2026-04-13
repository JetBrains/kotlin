// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// ISSUE: KT-22004

class A() {
    <!CONFLICTING_JVM_DECLARATIONS!>fun b() {
    }<!>

    @Deprecated("test", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_JVM_DECLARATIONS!>fun b() {
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, stringLiteral */

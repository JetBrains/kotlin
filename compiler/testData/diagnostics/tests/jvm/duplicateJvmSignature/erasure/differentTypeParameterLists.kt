// RUN_PIPELINE_TILL: CODEGEN
class Aaa() {
    <!CONFLICTING_JVM_DECLARATIONS!>fun f()<!> = 1
    <!CONFLICTING_JVM_DECLARATIONS!>fun <P> f()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
typeParameter */

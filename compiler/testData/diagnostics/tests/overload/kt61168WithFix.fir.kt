// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-61168

class A() {
}

@Deprecated("A", level = DeprecationLevel.HIDDEN)
fun A() = A()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, stringLiteral */

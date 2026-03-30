// RUN_PIPELINE_TILL: BACKEND
class C<T>
fun <T : C<T>> foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeConstraint, typeParameter */

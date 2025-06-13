// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

class Controller<T : Number> {
    suspend fun yield(t: T) {}
}

fun <S : Number> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test = generate {
    yield(<!TYPE_MISMATCH!>"foo"<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration,
stringLiteral, suspend, typeConstraint, typeParameter, typeWithExtension */

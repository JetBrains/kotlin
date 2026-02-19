// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47982

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>]<!>
    }
}




class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, functionDeclaration, functionalType, lambdaLiteral,
nullableType, typeParameter, typeWithExtension */

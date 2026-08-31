// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
fun test() {
    class `{x}`
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH!>`{x}`()<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass */

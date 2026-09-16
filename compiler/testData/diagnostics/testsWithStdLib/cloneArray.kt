// RUN_PIPELINE_TILL: CODEGEN
fun test_1(array: Array<String>) {
    array.clone()
}

fun test_2(array: IntArray) {
    array.clone()
}

/* GENERATED_FIR_TAGS: functionDeclaration */

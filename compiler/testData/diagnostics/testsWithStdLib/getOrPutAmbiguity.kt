// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK

fun test(map: MutableMap<Int, MutableMap<Int, Int>>) {
    map.getOrPut(1, ::mutableMapOf)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, integerLiteral */

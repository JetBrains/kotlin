// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

fun test(map: java.util.AbstractMap<String, Int>) {
    map.remove("", null)
    map.remove(null)
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib

// MODULE: main()()(lib)

fun nullIfEmpty(list: List<String>): List<String>? {
    return if (list.isNotEmpty()) {
        list
    } else {
        null
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, nullableType */

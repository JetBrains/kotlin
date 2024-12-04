// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
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
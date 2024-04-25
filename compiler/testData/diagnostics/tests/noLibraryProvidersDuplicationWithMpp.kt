// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib

// MODULE: lib2

// MODULE: main(lib2)()(lib)

fun nullIfEmpty(list: List<String>): List<String>? {
    return if (list.isNotEmpty()) {
        list
    } else {
        null
    }
}
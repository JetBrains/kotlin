// RUN_PIPELINE_TILL: BACKEND
package kotlinx.cinterop

@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class UnsafeNumber

// FILE: test.kt
@kotlinx.cinterop.UnsafeNumber
typealias size_t = Int

fun test(s: size_t) {
    s.toInt()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, typeAliasDeclaration */

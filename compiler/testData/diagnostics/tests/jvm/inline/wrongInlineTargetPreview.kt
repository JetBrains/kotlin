// RUN_PIPELINE_TILL: FRONTEND
// MODULE: library
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// FILE: a.kt
package a

inline fun inlineFun(p: () -> Unit) {
    p()
}

class C {
    inline fun inlineMember(p: () -> Unit) {
        p()
    }
}

// MODULE: main(library)
// JVM_TARGET: 17
// FILE: source.kt
package usage

import a.*

fun baz() {
    inlineFun {}
    C().inlineMember {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, lambdaLiteral */

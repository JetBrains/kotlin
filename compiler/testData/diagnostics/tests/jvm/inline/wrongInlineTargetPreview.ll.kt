// LL_FIR_DIVERGENCE
// `INLINE_FROM_HIGHER_PLATFORM` is not reported: LL analyzes the dependency module from sources, and
// `FirJvmInlineTargetQualifiedAccessChecker` skips source callables since only class files carry the version.
// LL_FIR_DIVERGENCE
// ISSUE: KT-89990
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

// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
package test

fun <T> T.noInline(p: (T) -> Unit) {
    p(this)
}

inline fun inlineCall(p: () -> Unit) {
    p()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val loci = listOf("a", "b", "c")
    var gene = "g1"

    inlineCall {
        val value = 10.0
        loci.forEach {
            var locusMap = 1.0
            {
                locusMap = value
                gene = "OK"
            }()
        }
    }
    return gene
}

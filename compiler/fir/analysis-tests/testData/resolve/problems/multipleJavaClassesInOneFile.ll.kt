// LL_FIR_DIVERGENCE
// two possible reasons:
// 1) LL FIR doesn't suffer from KT-4455 like standard Kotlin compiler does
// 2) LL FIR tests pass Java content roots to Kotlin compiler file-by-file instead of by a single folder
// LL_FIR_DIVERGENCE

// ISSUE: KT-4455

// FILE: foo/Some.java

package foo;

class Some {}

class Another {}

// FILE: main.kt
package foo

fun test() {
    val some = Some()
    val another = Another()
}

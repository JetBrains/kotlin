// IGNORE_FE10
// MODULE: StdlibImitation
// MODULE_KIND: LibrarySource
// COMPILER_ARGUMENTS: -Xallow-kotlin-package
// We want -Xallow-kotlin-package option to apply only during compilation, but not during analysis
// FILE: Sequence.kt
package kotlin.sequences

public interface Sequence<FAKE>

// FILE: utils.kt
package kotlin.collections

fun test(p: Sequenc<caret>e<*>) {}

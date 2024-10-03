// LL_FIR_DIVERGENCE
// `symbol.fir.sourceElement` is not set, so `FirJvmModuleAccessibilityQualifiedAccessChecker` doesn't run
// the code that should report the diagnostic.
// LL_FIR_DIVERGENCE
// ISSUE: KT-71943
// WITH_STDLIB

import sun.awt.image.*

fun withCustomDecoders(originalGetDecoder: () -> ImageDecoder?) {}
fun createImage() = withCustomDecoders { null }

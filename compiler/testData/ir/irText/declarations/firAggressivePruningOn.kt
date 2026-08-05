// DISABLE_WITH_PARSER: Psi
// FIR_AGGRESSIVE_PRUNING: true
// HEADER_MODE

// Category 1: Completely unused private member.
// - Kept under HEADER_MODE alone.
// - Pruned under FIR_AGGRESSIVE_PRUNING.
private val unusedPrivateProperty = "Unused"
private fun unusedPrivateFun() = "Unused"

// Category 2: Private member referenced ONLY by a public non-inline function.
// - Kept under HEADER_MODE alone (even though the referencing body is discarded).
// - Pruned under FIR_AGGRESSIVE_PRUNING (because the body is discarded, making it unreachable).
private val privatePropertyReferencedByNonInline = "ReferencedByNonInline"
private fun privateFunReferencedByNonInline() = "ReferencedByNonInline"

// Category 3: Private member referenced by a public inline function.
// - Kept under HEADER_MODE alone (inline body is preserved).
// - Kept under FIR_AGGRESSIVE_PRUNING (inline body is preserved, keeping it reachable).
private val privatePropertyReferencedByInline = "ReferencedByInline"
private fun privateFunReferencedByInline() = "ReferencedByInline"

// Public non-inline function (body is discarded in HEADER_MODE)
fun publicNonInlineFun(): String {
    return privateFunReferencedByNonInline() + privatePropertyReferencedByNonInline
}

// Public inline function (body is preserved in HEADER_MODE)
inline fun publicInlineFun(): String {
    return privateFunReferencedByInline() + privatePropertyReferencedByInline
}

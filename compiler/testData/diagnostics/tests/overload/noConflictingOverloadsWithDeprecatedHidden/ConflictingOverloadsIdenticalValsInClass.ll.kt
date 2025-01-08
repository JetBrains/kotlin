// LL_FIR_DIVERGENCE
//   LL test doesn't report backend diagnostics
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
class Aaa() {
    val a = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a = 1
}

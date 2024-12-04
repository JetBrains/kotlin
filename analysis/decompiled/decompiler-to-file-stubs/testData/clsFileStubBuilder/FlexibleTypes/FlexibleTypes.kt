// FIR_IGNORE
// Ignored for fir, as FIR does not support org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
/* ft is not available in common code */
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

package test

import kotlin.internal.flexible.ft

abstract class FlexibleTypes() {
    abstract fun collection(): ft<List<Int>, List<Any>>

    abstract val p: ft<Int, Int?>

    fun withBody(): ft<Int, Int?> { return 1 }
}
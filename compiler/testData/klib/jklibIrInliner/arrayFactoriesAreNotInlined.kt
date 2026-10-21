// The array factories are intrinsics lowered by the consumer of the KLIB, so they are not inlined.

fun test(): Any = arrayOf<Any>(emptyArray<String>(), intArrayOf(1))

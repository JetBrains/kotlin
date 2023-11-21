// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

annotation class Ann

sealed class Sealed @Ann constructor(@Ann val x: String)

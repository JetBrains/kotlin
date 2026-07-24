// SKIP_KT_DUMP

// KT-87709 Reflection: "invoke" in classes inheriting from suspend function types is loaded incorrectly
// KOTLIN_REFLECT_DUMP_MISMATCH

abstract class S0 : suspend () -> Unit

// CURIOUS_ABOUT: isNullableString, isNullableString2
// ISSUE: KT-615

// This test verifies the behavior of ConstantConditionEliminationMethodTransformer. The resulting bytecode should not contain `iconst_1`
// followed by a goto to conditional jump. In other words, the bytecode should be as compact as possible.

fun isNullableString(x: Any?) = if (x is String?) "true" else "false"
inline fun <reified T> isT(x: Any?) = if (x is T) "true" else "false"
fun isNullableString2(x: Any?) = isT<String?>(x)

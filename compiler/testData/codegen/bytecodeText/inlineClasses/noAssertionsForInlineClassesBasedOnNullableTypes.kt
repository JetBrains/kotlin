// LANGUAGE: +InlineClasses

inline class AsAny(val a: Any?)

fun asNotNullAny(a: AsAny) {}
fun AsAny.asNotNullAnyExtension(b: AsAny): AsAny = this

// 0 checkParameterIsNotNull
// 0 checkNotNullParameter

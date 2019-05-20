// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class AsAny(val a: Any?)

fun asNotNullAny(a: AsAny) {}
fun AsAny.asNotNullAnyExtension(b: AsAny): AsAny = this

// 0 checkParameterIsNotNull
// WITH_RUNTIME
// The remaining differences of JVM and JVM_IR here are reported at KT-36970, KT-41841.

@file:JvmMultifileClass
@file:JvmName("A")

private fun foo(x: String = "") {}

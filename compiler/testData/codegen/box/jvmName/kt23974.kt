// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-69075
// WITH_STDLIB

@Suppress("x")
@get:JvmName("foo")
val vo get() = "O"

@Suppress("x")
@get:JvmName("bar")
val vk = "K"

fun box() = vo + vk
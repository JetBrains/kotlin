// TARGET_BACKEND: JVM
// WITH_RUNTIME

@Suppress("x")
@get:JvmName("foo")
val vo get() = "O"

@Suppress("x")
@get:JvmName("bar")
val vk = "K"

fun box() = vo + vk
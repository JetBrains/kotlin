// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR

@JvmInline
value class Some(val x: Other)

//@JvmInline
data class Other(val x: Int)

fun box(): String = "OK"

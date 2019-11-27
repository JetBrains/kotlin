// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class C<K> @JvmOverloads constructor(val s: String = "OK")

fun box() = C<Unit>().s
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class C<K> @JvmOverloads constructor(val s: String = "OK")

fun box() = C<Unit>().s

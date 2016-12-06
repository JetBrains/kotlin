// WITH_RUNTIME
// IGNORE_BACKEND: JS

object Host {
    class StringDelegate(val s: String) {
        operator fun getValue(receiver: String, p: Any) = receiver + s
    }

    operator fun String.toDelegateFor(host: Any?, p: Any) = StringDelegate(this)

    val String.plusK by "K"

    val ok = "O".plusK
}

fun box(): String = Host.ok
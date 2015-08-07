// NO_KOTLIN_REFLECT

import kotlin.reflect.*

data class Box(val value: String)

var pr = Box("first")

fun box(): String {
    val p = ::pr
    if (p.get().value != "first") return "Fail value 1: ${p.get()}"
    if (p.name != "pr") return "Fail name: ${p.name}"
    p.set(Box("second"))
    if (p.get().value != "second") return "Fail value 2: ${p.get()}"
    return "OK"
}

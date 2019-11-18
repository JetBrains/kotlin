// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// FULL_JDK

open class B(val map: LinkedHashMap<String, String>)

class C : B(linkedMapOf("O" to "K"))

fun box() =
        C().map.entries.first().let { it.key + it.value }
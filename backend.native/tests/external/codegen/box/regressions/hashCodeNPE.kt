// See KT-14242
var x = 1
fun box(): String {
    val any: Any? = when (1) {
        x -> null
        else -> Any()
    }

    // Must not be NPE here
    val hashCode = any?.hashCode()

    return hashCode?.toString() ?: "OK"
}

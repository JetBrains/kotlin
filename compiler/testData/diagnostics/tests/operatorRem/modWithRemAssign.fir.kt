// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

class ModAndRemAssign {
    operator fun mod(x: Int) = ModAndRemAssign()
    operator fun mod(x: String) = ModAndRemAssign()
    operator fun modAssign(x: String) {}
    operator fun remAssign(x: Int) {}
}

operator fun ModAndRemAssign.mod(x: String) = ModAndRemAssign()
operator fun ModAndRemAssign.modAssign(x: String) {}

fun test() {
    val modAndRemAssign = ModAndRemAssign()
    modAndRemAssign %= 1
}
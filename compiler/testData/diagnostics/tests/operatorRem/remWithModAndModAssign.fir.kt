// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

class ModAndRemAssign {
    operator fun mod(x: Int) = ModAndRemAssign()
    operator fun mod(x: String) = ModAndRemAssign()
    operator fun modAssign(x: String) {}
    operator fun rem(x: Int) = ModAndRemAssign()
}

operator fun ModAndRemAssign.mod(x: String) = ModAndRemAssign()
operator fun ModAndRemAssign.modAssign(x: String) {}

fun test() {
    var modAndRemAssign = ModAndRemAssign()
    modAndRemAssign %= 1
}
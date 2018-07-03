// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

class ModAndRemAssign {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) = ModAndRemAssign()
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: String) = ModAndRemAssign()
    <!DEPRECATED_BINARY_MOD!>operator<!> fun modAssign(x: String) {}
    operator fun rem(x: Int) = ModAndRemAssign()
}

<!DEPRECATED_BINARY_MOD!>operator<!> fun ModAndRemAssign.mod(x: String) = ModAndRemAssign()
<!DEPRECATED_BINARY_MOD!>operator<!> fun ModAndRemAssign.modAssign(x: String) {}

fun test() {
    var modAndRemAssign = ModAndRemAssign()
    modAndRemAssign %= 1
}
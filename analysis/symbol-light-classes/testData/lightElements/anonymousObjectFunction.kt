// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(second)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(second)
fun test() {
    val o = object {
        fun first() {}
        fun sec<caret>ond(p: Int) {}
        fun second() {}
    }
}

// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
// LANGUAGE: +FullValueClasses
value class ValueClass(val first: Int, val second: Int) {
    fun mem<caret>ber(): Int = first
}

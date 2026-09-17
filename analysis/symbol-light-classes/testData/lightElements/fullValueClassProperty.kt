// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
// LANGUAGE: +FullValueClasses
value class ValueClass(val first: Int, val second: Int) {
    val prop<caret>erty: Int get() = first
}

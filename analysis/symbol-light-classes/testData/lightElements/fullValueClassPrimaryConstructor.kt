// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
// LANGUAGE: +FullValueClasses
value class ValueClass<caret>(val first: Int = 0, val second: Int = 0)

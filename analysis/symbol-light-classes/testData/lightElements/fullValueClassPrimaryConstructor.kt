// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor(ValueClass)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor(ValueClass)
// LANGUAGE: +FullValueClasses
value class ValueClass<caret>(val first: Int = 0, val second: Int = 0)

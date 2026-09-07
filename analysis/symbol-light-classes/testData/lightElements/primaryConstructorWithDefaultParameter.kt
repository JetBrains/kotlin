// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor(Foo)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor(Foo)
class Foo<caret>(var p: String = "42")
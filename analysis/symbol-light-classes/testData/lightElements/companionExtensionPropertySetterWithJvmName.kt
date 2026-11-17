// LANGUAGE: +CompanionBlocks +CompanionExtensions
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(renamedSetter)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(renamedSetter)
class Foo

companion var Foo.property: Int
    get() = 2
    @JvmName("renamedSetter")
    s<caret>et(value) {}

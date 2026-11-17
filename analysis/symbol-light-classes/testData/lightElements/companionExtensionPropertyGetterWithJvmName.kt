// LANGUAGE: +CompanionBlocks +CompanionExtensions
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(renamedGetter)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(renamedGetter)
class Foo

companion val Foo.property: Int
    @JvmName("renamedGetter")
    g<caret>et() = 2

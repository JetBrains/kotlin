// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
class Foo {
    @get:JvmName("getBar")
    val <caret>p: Int = 42
}
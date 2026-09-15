// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry(AA)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry(AA)

enum class MyEnum {
    A<caret>A {
        override fun toString(): String = "AA"
    }
}

// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry(BB)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry(BB)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry(BB)

enum class MyEnum {
    A,
    B<caret>B {
        override fun toString(): String = "BB"
    },
    C,
    D {
        override fun toString(): String = "D"
    }
}

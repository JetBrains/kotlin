class TopLevel {
    deprecated("Nested")
    class Nested {
        companion object {
            fun use() {}

            class CompanionNested2
        }

        class Nested2
    }
}

fun useNested() {
    val <!UNUSED_VARIABLE!>d<!> = TopLevel.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Nested<!>.use()
    TopLevel.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Nested<!>.Nested2()
    TopLevel.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Nested<!>.<!UNRESOLVED_REFERENCE!>CompanionNested2<!>()
}
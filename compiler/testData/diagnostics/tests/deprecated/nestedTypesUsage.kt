class TopLevel {
    @Deprecated("Nested")
    class Nested {
        companion object {
            fun use() {}

            class CompanionNested2
        }

        class Nested2
    }
}

fun useNested() {
    val <!UNUSED_VARIABLE!>d<!> = TopLevel.<!DEPRECATION!>Nested<!>.use()
    TopLevel.<!DEPRECATION!>Nested<!>.Nested2()
    TopLevel.<!DEPRECATION!>Nested<!>.<!UNRESOLVED_REFERENCE!>CompanionNested2<!>()
}
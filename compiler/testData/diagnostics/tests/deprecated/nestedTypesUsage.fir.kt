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
    val d = TopLevel.Nested.use()
    TopLevel.Nested.Nested2()
    TopLevel.Nested.CompanionNested2()
}
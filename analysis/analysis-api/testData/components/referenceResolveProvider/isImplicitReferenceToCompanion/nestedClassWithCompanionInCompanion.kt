class AA {
    companion object {
        class NestedInCompanion {
            companion object
        }
    }
}

fun main() {
    AA.Companion.Nested<caret>InCompanion
}
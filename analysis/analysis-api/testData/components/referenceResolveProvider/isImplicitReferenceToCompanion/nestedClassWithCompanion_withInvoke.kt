class AA {
    interface Nested {
        companion object {
            operator fun invoke() {}
        }
    }
}

fun main() {
    AA.Nes<caret>ted()
}
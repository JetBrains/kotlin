class AA {
    interface Nested {
        companion object {
            operator fun invoke() {}
        }
    }
}

fun main() {
    A<caret>A.Nested()
}
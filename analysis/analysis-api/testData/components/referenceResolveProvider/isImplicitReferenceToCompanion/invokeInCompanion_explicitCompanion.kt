interface AA {
    companion object {
        operator fun invoke() {}
    }
}

fun main() {
    AA.Comp<caret>anion()
}


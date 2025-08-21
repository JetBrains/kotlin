package pack

annotation class Anno(val s: String)

class TopLevelClass(val i: Int) {
    <expr>@Anno(CONSTANT)</expr>
    class NestedClass(val d: Double) {
        companion object {
            const val CONSTANT = 1
        }
    }

    companion object {
        const val CONSTANT = "str"
    }
}

package pack

annotation class Anno(val s: String)

enum class MyEnumClass(val i: Int) {
    <expr>@Anno(CONSTANT)</expr> ENTRY(0);

    companion object {
        const val CONSTANT = "str"
    }
}

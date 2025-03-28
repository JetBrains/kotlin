package pack

annotation class Anno(val s: String)

enum class MyEnumClass(val i: Int) {
    @Anno(<expr>CONSTANT</expr>) ENTRY(1);

    companion object {
        const val CONSTANT = "str"
    }
}

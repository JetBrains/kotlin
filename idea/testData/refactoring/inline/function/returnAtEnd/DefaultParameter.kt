fun provideSimple() = listOf("statement 0")
fun provideComplex(): MutableList<String> {
    val result = mutableListOf("statement 1")
    result.add("statement 2")
    return result
}
fun callInDefault(simple: List<String> = provideSimple(), complex: List<String> = <caret>provideComplex()) {
}
// !LANGUAGE: +NestedClassesInAnnotations

annotation class Foo {
    class Nested

    inner class Inner

    enum class E { A, B }
    object O
    interface I
    annotation class Anno(val e: E)

    companion object {
        val x = 1
        const val y = ""
    }


    constructor(s: Int) {}
    init {}
    fun function() {}
    val property get() = Unit
}

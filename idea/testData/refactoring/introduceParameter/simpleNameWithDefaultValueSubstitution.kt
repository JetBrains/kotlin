// WITH_DEFAULT_VALUE: false

class A {
    val prop = ""

    fun foo() = bar("")

    private fun bar(x: String): Boolean {
        <selection>prop</selection>
        return true
    }
}
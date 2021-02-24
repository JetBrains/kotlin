class Test {
    val test = when {<caret>foo()

    fun foo(): Int {
        return 42
    }
}
//-----
class Test {
    val test = when {
        <caret>foo()
    }

    fun foo(): Int {
        return 42
    }
}
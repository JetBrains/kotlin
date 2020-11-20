// WITH_RUNTIME
class Test {
    val test = run {<caret>foo()

    fun foo(): Int {
        return 42
    }
}
//-----
// WITH_RUNTIME
class Test {
    val test = run {
        <caret>foo()
    }

    fun foo(): Int {
        return 42
    }
}
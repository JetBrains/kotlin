// PROBLEM: none
class C {
    private val v = 1

    <caret>@Deprecated("")
    fun foo() {
        bar(v)
    }

    fun bar(p: Int){}
}

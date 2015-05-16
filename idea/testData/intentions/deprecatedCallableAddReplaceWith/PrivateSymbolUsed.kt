// IS_APPLICABLE: false
class C {
    private val v = 1

    <caret>@deprecated("")
    fun foo() {
        bar(v)
    }

    fun bar(p: Int){}
}

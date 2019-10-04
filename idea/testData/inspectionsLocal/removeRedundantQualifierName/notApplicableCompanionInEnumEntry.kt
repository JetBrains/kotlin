// PROBLEM: none
enum class C(val i: Int) {
    ONE(<caret>C.K)
    ;

    companion object {
        const val K = 1
    }
}
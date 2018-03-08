// PROBLEM: none
class C {
    companion object {
        fun create() = C()
    }
}

fun test() {
    <caret>C.create()
}
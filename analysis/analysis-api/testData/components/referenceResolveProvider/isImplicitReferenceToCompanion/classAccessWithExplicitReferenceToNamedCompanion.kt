class AA {
    companion object BB {
        fun x() = 10
    }
}
fun main() {
    A<caret>A.BB.x()
}


class AA {
    companion object BB {
        fun x() = 10
    }
}
fun main() {
    AA.B<caret>B.x()
}


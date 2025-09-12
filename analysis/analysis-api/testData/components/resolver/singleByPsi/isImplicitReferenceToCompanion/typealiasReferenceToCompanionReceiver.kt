class AA {
    companion object {
        fun x() = 10
    }
}

typealias TA = AA

fun main() {
    T<caret>A.x()
}
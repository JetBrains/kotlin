interface AA {
    companion object {
        operator fun invoke() {}
    }
}

typealias TA = AA

fun main() {
    T<caret>A()
}


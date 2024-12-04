class AA {
    companion object {
        operator fun invoke() {}
    }
}

fun main() {
    A<caret>A()
}


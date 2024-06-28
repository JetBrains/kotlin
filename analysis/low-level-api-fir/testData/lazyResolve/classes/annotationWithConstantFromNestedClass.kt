annotation class Anno(val number: Int)

class Outer {
    companion object {
        const val CONSTANT_FROM_COMPANION = 42

        @Anno(CONSTANT_FROM_COMPANION)
        class N<caret>ested
    }
}

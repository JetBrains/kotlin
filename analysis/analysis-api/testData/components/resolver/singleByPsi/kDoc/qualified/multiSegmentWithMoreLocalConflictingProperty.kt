interface A {
    companion object {
        val BB: Int = 1
    }
}

class Usage1 {
    val A: Int = 1

    class Nested { // Not inner
        /**
         * [A.B<caret_1>B]
         */
        fun foo() {}
    }
}


class Usage2 {
    val A: Int = 1

    inner class Nested { // Inner
        /**
         * [A.B<caret_2>B]
         */
        fun foo() {}
    }
}
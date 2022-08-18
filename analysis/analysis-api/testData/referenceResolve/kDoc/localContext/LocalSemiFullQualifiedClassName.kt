// COMPILATION_ERRORS
fun f() {
    class C {
        class X {
            class YY {
                fun aa() {}
            }
        }

        /**
         * [X.Y<caret>Y]
         */
        fun g() {

        }
    }
}
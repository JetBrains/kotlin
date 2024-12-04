// COMPILATION_ERRORS
fun f() {
    class C {
        class X {
            class YY {
                fun aa() {}
            }
        }

        /**
         * [X.YY.a<caret>a]
         */
        fun g() {

        }
    }
}
class C {
    class X {
        companion object {
            class YY {
                fun aa() {}
            }
        }
    }

    /**
     * [X.YY.a<caret>a]
     */
    fun g() {

    }
}

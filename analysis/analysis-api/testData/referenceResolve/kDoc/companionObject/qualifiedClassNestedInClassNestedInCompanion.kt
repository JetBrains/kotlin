class C {
    class X {
        companion object {
            class YY {
                class ZZ { }
            }
        }
    }

    /**
     * [X.YY.Z<caret>Z]
     */
    fun g() {

    }
}

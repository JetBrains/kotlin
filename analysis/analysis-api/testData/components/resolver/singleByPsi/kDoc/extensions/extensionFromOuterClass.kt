class A {
    fun Int.foo() {}

    /**
     * [Int.fo<caret_1>o]
     */
    class B {
        /**
        * [Int.fo<caret_2>o]
        */
        fun usage() {}
    }

    /**
     * [Int.fo<caret_3>o]
     */
    inner class C {
        /**
        * [Int.fo<caret_4>o]
        */
        fun usage() {}
    }

    /**
     * [Int.fo<caret_5>o]
     */
    companion object {
        /**
        * [Int.fo<caret_6>o]
        */
        fun usage() {}
    }
}
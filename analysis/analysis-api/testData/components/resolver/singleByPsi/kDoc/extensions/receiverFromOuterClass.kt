class A {
    class Some

    /**
     * [Some.fo<caret_1>o]
     */
    class B {
        /**
         * [Some.fo<caret_2>o]
         */
        fun Some.foo() {}
    }

    /**
     * [Some.fo<caret_3>o]
     */
    inner class C {
        /**
         * [Some.fo<caret_4>o]
         */
        fun Some.foo() {}
    }

    /**
     * [Some.fo<caret_5>o]
     */
    companion object {
        /**
         * [Some.fo<caret_6>o]
         */
        fun Some.foo() {}
    }
}
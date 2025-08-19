class A<TT> {
    class B {
        /**
         * [T<caret_1>T]
         */
        fun usage() {}
    }

    inner class C {
        /**
         * [T<caret_2>T]
         */
        fun usage() {}
    }
}
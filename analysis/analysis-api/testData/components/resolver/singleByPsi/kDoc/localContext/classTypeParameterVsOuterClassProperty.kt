interface A {
    val TT: Int

    class A<TT> {

        /**
         * [T<caret>T]
         */
        fun usage() {}
    }
}
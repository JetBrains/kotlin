class A {
	private val foo = 6

    /**
     * [f<caret_1>oo]
     */
	companion object {
        /**
         * [f<caret_2>oo]
         */
		fun usage() {}
	}

    /**
     * [fo<caret_3>o]
     */
    class B {
        /**
         * [fo<caret_4>o]
         */
		fun usage() {}
    }

    /**
     * [fo<caret_5>o]
     */
    inner class C {
        /**
         * [fo<caret_6>o]
         */
		fun usage() {}
    }
}
package test

class Test {

    val test0 = 42


    /**
     * comment
     */
    val test1 = 42


    @Suppress(
        "UNUSED_VARIABLE"
    )
    val test2 = 42


    private
    val test3 = 42


    val test4 get() = 42


    val test5
        get() = 42


    val test6
        /**
         * comment
         */
        get() = 42


    val test7
        @Suppress(
            "UNUSED_VARIABLE"
        )
        get() = 42


    var test8 = 42


    var test9 = 42; private set


    var test10 = 42
        private set


    var test11 = 42
        set(value) {
            field = value
        }


    var test12 = 42
        /**
         * comment
         */
        set(value) {
            field = value
        }


    var test13 = 42
        @Suppress(
            "UNUSED_VARIABLE"
        )
        set(value) {
            field = value
        }

}
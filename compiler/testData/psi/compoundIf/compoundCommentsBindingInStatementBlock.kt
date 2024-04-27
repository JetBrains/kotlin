fun test() {
    if (val x = 10; x > 0) {
        /*start-end*/
    }


    if (val y = 5 * 2; y == 10) {
        /*start-end*/
    }


    if (val z = 3; val w = 9; z < w) {
        /*start*/
        /*end*/
    }


    if (val a = 12; a > 10) {
        /*start*/

        /** doc */
        val b = a * 2  // Using 'a' to calculate a new value

        /*end*/
    }
}

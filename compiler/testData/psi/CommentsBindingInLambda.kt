val la1 = {
    // start
    // start 1
    foo()

    // middle

    foo()

    // end
}

val la2 = {
    /**/
}

val la3 = {
    /** */
}

val la4 = {
    /** Should be under block */

    /** Should be under property */
    val some = 1
}

val la5 = {
    /** */
    /** */
}

val la6 = /*1*/ {/*2*/ a /*3*/ -> /*4*/
}

val la7 = {/**/}

fun foo() {}

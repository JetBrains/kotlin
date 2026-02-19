// WITH_STDLIB
// MODULE: main
// FILE: main.kt

/**
 * Class KDoc
 *
 * @property a my property
 * @property b my property
 *
 * @throws Exception when necessary
 */
class Foo(val a: String, val b: Int = 0) {
    /**
     * Secondary constructor docs
     *
     * @param a parameter
     */
    constructor(a: String) : this(a) {}

    /**
     * Property KDoc
     */
    val property: String

    /**
     * Fun KDoc
     *
     * @param a param
     * @return abs value
     *
     * @see [kotlin.math]
     * @throws Throwable
     */
    fun foo(a: String): Int {
        return a.length
    }
}
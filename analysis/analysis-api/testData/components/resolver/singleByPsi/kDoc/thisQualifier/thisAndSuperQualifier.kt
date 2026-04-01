open class Base {
    val `super`: Int = 1
    val `this`: String get() = "str"
}

class Impl : Base() {
    /**
     * [sup<caret_1>er]
     * [`sup<caret_2>er`]
     * [th<caret_3>is]
     * [`th<caret_4>is`]
     */
    fun noReceiver() {

    }

    /**
     * [th<caret_5>is]
     * [`th<caret_6>is`]
     */
    fun Long.withReceiver() {

    }
}

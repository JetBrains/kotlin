package test

class Receiver

open class Base {
    fun Receiver.ext() {}
}

/**
 * [Receiver.<caret_1>ext]
 */
class Child : Base() {
    /**
     * [Receiver.<caret_2>ext]
     */
    fun usage() {}
}
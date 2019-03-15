// ULTRA_LIGHT_CLASSES
package test

expect interface Closable {
    fun <caret>close()
}

expect class MyStream : Closable {}

open class MyImpl : Closable {
    override fun close() {}
}

// REF: [testModule_Common] (in test.MyImpl).close()

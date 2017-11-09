// WITH_RUNTIME

import java.io.Closeable

class MyCloseable : Closeable {
    override fun close() {}

    fun process(x: Int) = x

    fun Int.foo() {
        <caret>try {
            this@MyCloseable.process(this)
        }
        finally {
            this@MyCloseable.close()
        }
    }
}
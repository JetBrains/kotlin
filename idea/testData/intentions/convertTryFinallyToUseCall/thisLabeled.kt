// WITH_RUNTIME

import java.io.Closeable

class MyCloseable : Closeable {
    override fun close() {}

    fun process(x: Int) = x

    fun Int.foo() {
        try {
            this@MyCloseable.process(this)
        }
        <caret>finally {
            this@MyCloseable.close()
        }
    }
}
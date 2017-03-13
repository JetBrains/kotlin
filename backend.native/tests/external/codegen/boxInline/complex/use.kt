// FILE: 1.kt

package test

public class Data()

public class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}
public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public interface Closeable {
    open public fun close() {}
}

public inline fun <T: Closeable, R> T.use(block: (T)-> R) : R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {

        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}


public fun Input.copyTo(output: Output, size: Int): Long {
    return output.doOutput(this.data()).toLong()
}

// FILE: 2.kt

import test.*


fun Data.test1(d: Data) : Long  {
    val input2 = Input(this)
    val input = Input(this)
    return input.use<Input, Long>{
        val output = Output(d)
        output.use<Output,Long>{
            input.copyTo(output, 10)
        }
    }
}


fun box(): String {

    val result = Data().test1(Data())
    if (result != 100.toLong()) return "test1: ${result}"

    return "OK"
}

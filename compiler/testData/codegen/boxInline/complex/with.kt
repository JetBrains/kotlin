// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_RUNTIME
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

public inline fun <R> use(block: ()-> R) : R {
    return block()
}

public fun <R> useNoInline(block: ()-> R) : R {
    return block()
}


public fun Input.copyTo(output: Output, size: Int): Long {
    return output.doOutput(this.data()).toLong()
}


public inline fun <T> with2(receiver : T, crossinline body :  T.() -> Unit) : Unit = {receiver.body()}()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun Data.test1(d: Data) : Long  {
    val input = Input(this)
    var result = 10.toLong()
    with(input) {
         result = use<Long>{
            val output = Output(d)
             use<Long>{
                data()
                copyTo(output, 10)
            }
        }
    }
    return result
}

fun Data.test2(d: Data) : Long  {
    val input = Input(this)
    var result = 10.toLong()
    with2(input) {
        result = use<Long>{
            val output = Output(d)
            useNoInline<Long>{
                data()
                copyTo(output, 10)
            }
        }
    }
    return result
}


fun box(): String {

    val result = Data().test1(Data())
    if (result != 100.toLong()) return "test1: ${result}"

    val result2 = Data().test2(Data())
    if (result2 != 100.toLong()) return "test2: ${result2}"

    return "OK"
}

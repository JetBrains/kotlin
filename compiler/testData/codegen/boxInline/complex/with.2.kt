package test

public class Data()

public class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}
public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public open trait Closeable {
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


public inline fun with2<T>(receiver : T, body :  T.() -> Unit) : Unit = {receiver.body()}()

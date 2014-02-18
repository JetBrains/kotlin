package test

public class Data(val value: Int)

public class Input(val d: Data)  {
    public fun data() : Int = 100
}

public inline fun <R> use(block: ()-> R) : R {
    return block()
}


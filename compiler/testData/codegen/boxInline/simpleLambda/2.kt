package test

public class Data()

public inline fun <T, R> T.use(block: (T)-> R) : R {
    return block(this)
}

public inline fun use2() : Int {
    val s = 100
    return s
}



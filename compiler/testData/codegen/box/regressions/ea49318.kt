fun example(value : Int) {
    val result: Int = if (value == 0) 1
    else if (value == 1) 2
    else throw IllegalArgumentException()
    result
}

fun <T, S, U : T> foo(u: U) where U : S {}


fun main(args : Array<String>) {
    foo(null!!)
}


fun box() = "OK"
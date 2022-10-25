fun <K> first(a: K, b: K) = a

fun mox(): String {
    return <!TYPE_MISMATCH!>first(false, "fail:")<!> // must be error
}

fun box(): String {
    return if (true) <!TYPE_MISMATCH!>{
        val a = 1 // must be an error
    }<!> else {
        "fail:"
    }
}

fun main(args: Array<String>) {
    val rest: String = box() // must be ok
    val nest: String = mox() // must be ok
}

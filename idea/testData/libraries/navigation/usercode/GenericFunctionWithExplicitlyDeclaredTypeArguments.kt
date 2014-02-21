import testData.libraries.*

fun test() {
    genericFunc<String>()
}

// main.kt
//public fun <T> <1>genericFunc() : T = throw Exception()
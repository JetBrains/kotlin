import testData.libraries.*

fun foo() {
    println("".exProp)
    val p = Pair(1, 2)
    println(p.exProp)
}

// main.kt
//public val String.<1>exProp : String
//get() {
//    return this
//}
//
//public val Int.exProp : Int
//get() {
//    return this
//}
//
//public val <T> Pair<T, T>.<2>exProp : String
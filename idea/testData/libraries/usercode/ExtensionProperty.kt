import testData.libraries.*

fun foo() {
    println("".exProp)
    println(#(1, 2).exProp)
}

// library.kt
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
//public val <T> #(T, T).<2>exProp : String
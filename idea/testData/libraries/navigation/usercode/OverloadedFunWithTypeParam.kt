import testData.libraries.*

fun main(args: Array<String>) {
    funWithTypeParam(1)
    funWithTypeParam("")
}

// main.kt
//public fun <T: CharSequence> <2>funWithTypeParam(t: T) {
//}
//
//public fun <T: Number> <1>funWithTypeParam(t: T) {

import testData.libraries.*

fun foo() {
    func("5")
    func(5)
}

// extra.kt
//public fun <1>func(str : String) {
// main.kt
//public fun <2>func(a : Int, b : String = "55") {

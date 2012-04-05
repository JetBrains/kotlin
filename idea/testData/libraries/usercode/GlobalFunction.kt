import testData.libraries.*

fun foo() {
    func(5)
    func(5, "5")
    func(5, 5)
    func()
}

// library.kt
//public fun <1><2>func(a : Int, b : String = "55") {
//}
//
//public fun <3>func(a : Int, b : Int) {
//}
//
//public fun <4>func() {

// "Add non-null asserted (!!) call" "true"
class C {
    val s: String? = null
}

// Test for KTIJ-10052
fun C.test() {
    var z: String = <caret>s
}
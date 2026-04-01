// COMPILATION_ERRORS
// IGNORE_ERRORS_FROM_API: KT-85154

class InitOrderDemo(name: String) {
    val (firstProperty = "First property"
}

class InitOrderDemo2 {
    val (firstProperty = object : Int {}
}

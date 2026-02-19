// COMPILATION_ERRORS

class InitOrderDemo(name: String) {
    val (firstProperty = "First property"
}

class InitOrderDemo2 {
    val (firstProperty = object : Int {}
}
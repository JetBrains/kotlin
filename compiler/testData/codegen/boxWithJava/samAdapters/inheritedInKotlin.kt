class KotlinSubclass: JavaClass() {
}

fun box(): String {
    var v = "FAIL"
    KotlinSubclass().run { v = "OK" }
    return v
}
// KT-5912
fun box(): String {
    var s = "Failt"
    JavaClass<String>().perform("") { s = "OK" }
    return s
}

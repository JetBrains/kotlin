fun box(): String {
    var s = "Failt"
    kt5912<String>().perform("") { s = "OK" }
    return s
}
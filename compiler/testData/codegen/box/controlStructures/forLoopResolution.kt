fun box(): String {
    var result = ""
    for (i in 1..10) {
        result += i
    }
    if (result != "12345678910") throw AssertionError(result)
    return "OK"
}

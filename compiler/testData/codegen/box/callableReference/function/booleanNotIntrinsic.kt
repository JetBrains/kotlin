fun box(): String {
    if ((Boolean::not).let { it(true) } != false) return "Fail 1"
    if ((Boolean::not).let { it(false) } != true) return "Fail 2"
    return "OK"
}

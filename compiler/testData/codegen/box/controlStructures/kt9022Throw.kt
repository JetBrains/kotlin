fun box(): String {
    var cycle = true;
    while (true) {
        if (true || throw java.lang.RuntimeException()) {
            return "OK"
        }
    }
    return "fail"
}
fun box(): String {
    var i = 10
    while (true) {
        i = i - 1
        if (i == 0) {
            break
        } else {
            continue
        }
    }
    return "OK"
}
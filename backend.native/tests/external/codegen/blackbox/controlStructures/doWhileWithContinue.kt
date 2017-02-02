fun box(): String {
    var i = 0
    do {
        if (i++ > 100) break;
        continue;
    } while(false)
    if (i != 1) return "Fail 1, expected 1, but $i"

    i = 0
    do {
        if (i++ > 100) break;
        continue;
    } while(i<10)
    if (i != 10) return "Fail 2, expected 10, but $i"

    i = 0
    do continue while(i++<10)
    if (i != 11) return "Fail 3, expected 11, but $i"

    return "OK"
}

fun t1(b: Boolean) {
    var u: String
    if (val  x = "s"; b) {
        u = x
    }
    doSmth(u)

    var r: String
    if (val x = "s"; b) {
        r = x
    }
    else {
        r = x
    }
    doSmth(r)
}

fun t2(b: Boolean) {
    val i = 3
    if (val x = "s"; b) {
        return;
    }
    doSmth(i)
    if (val y = "e"; i is Int) {
        return;
    }
}

fun doSmth(s: String) {}



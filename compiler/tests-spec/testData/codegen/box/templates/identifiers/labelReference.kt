fun <!ELEMENT(3)!>() {
    val l = <!ELEMENT(2)!>@ {
        return@<!ELEMENT(2)!>
    }
    l()
    return@<!ELEMENT(1)!>
}

fun box(): String? {
    <!ELEMENT(3)!>()

    var i = 0
    <!ELEMENT(2)!>@ while (i < 10) {
        i++
        if (i <= 7) {
            continue@<!ELEMENT(2)!>
        }
        if (i > 5) {
            break@<!ELEMENT(2)!>
        }
    }

    return "OK"
}

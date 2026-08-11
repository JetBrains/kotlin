// WITH_STDLIB
fun box(): String {
    var r = ""
    outer@ for (i in 0..2) {
        for (x in sequence { yield(1); yield(2); yield(3) }) {
            if (x == 2) continue@outer
            if (i == 2) break@outer
            r += "$i$x"
        }
    }
    return if (r == "0111") "OK" else "Fail: $r"
}

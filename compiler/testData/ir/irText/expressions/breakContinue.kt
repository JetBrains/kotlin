fun test1() {
    while (true) { break }
    do { break } while (true)
    while (true) { continue }
    do { continue } while (true)
}

fun test2() {
    OUTER@while(true) {
        INNER@while(true) {
            break@INNER
            break@OUTER
        }
        break@OUTER
    }
    OUTER@while(true) {
        INNER@while(true) {
            continue@INNER
            continue@OUTER
        }
        continue@OUTER
    }
}

fun test3() {
    L@while(true) {
        L@while(true) {
            break@L
        }
        break@L
    }
    L@while(true) {
        L@while(true) {
            continue@L
        }
        continue@L
    }
}


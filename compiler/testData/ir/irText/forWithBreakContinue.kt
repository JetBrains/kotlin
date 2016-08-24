fun testForBreak1(ss: List<String>) {
    for (s in ss) {
        break
    }
}

fun testForBreak2(ss: List<String>) {
    OUTER@for (s1 in ss) {
        INNER@for (s2 in ss) {
            break@OUTER
            break@INNER
            break
        }
        break@OUTER
    }
}

fun testForContinue1(ss: List<String>) {
    for (s in ss) {
        continue
    }
}

fun testForContinue2(ss: List<String>) {
    OUTER@for (s1 in ss) {
        INNER@for (s2 in ss) {
            continue@OUTER
            continue@INNER
            continue
        }
        continue@OUTER
    }
}

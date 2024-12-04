fun test1(c: Boolean?) {
    L@ while (true) {
        L2@while (c ?: break) ;
    }
}

fun test2(c: Boolean?) {
    L@ while (true) {
        L2@while (c ?: continue) ;
    }
}

fun test3(ss: List<String>?) {
    L@ while (true) {
        L2@for (s in ss ?: continue) ;
    }
}

fun test4(ss: List<String>?) {
    L@ while (true) {
        L2@for (s in ss ?: break) ;
    }
}

fun test5() {
    var i = 0
    Outer@while (true) {
        ++i
        var j = 0
        Inner@do {
            ++j
        } while (if (j >= 3) false else break) // break@Inner
        if (i == 3) break
    }
}

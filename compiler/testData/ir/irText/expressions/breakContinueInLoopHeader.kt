fun test1(c: Boolean?) {
    L@ while (true) {
        while (c ?: break)
    }
}

fun test2(c: Boolean?) {
    L@ while (true) {
        while (c ?: continue)
    }
}

fun test3(ss: List<String>?) {
    L@ while (true) {
        for (s in ss ?: continue)
    }
}

fun test4(ss: List<String>?) {
    L@ while (true) {
        for (s in ss ?: break)
    }
}
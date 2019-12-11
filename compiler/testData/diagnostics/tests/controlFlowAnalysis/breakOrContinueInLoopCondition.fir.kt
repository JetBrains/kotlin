fun test() {

    l@ for (i in if (true) 1..10 else continue@l) {}
    for (i in if (true) 1..10 else continue) {}

    while (break) {}
    l@ while (break@l) {}

    do {} while (continue)
    l@ do {} while (continue@l)

    //KT-5704
    var i = 0
    while (if(i++ == 10) break else continue) {}
}

fun test2(b: Boolean) {
    while (b) {
        while (break) {}
    }

    do {
        while (continue) {}
    } while (b)

    while (b) {
        do {} while (break)
    }

    for (i in 1..10) {
        for (j in if (true) 1..10 else continue) {
        }
    }
}
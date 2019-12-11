//KT-1027 Strange selection of unreachable code

package kt1027

fun foo(c: List<Int>) {
    var i = 2

    return

    for (j in c) {  //strange selection of unreachable code
        i += 23
    }
}

fun t1() {
    return

    while(true) {
        doSmth()
    }
}

fun t2() {
    return

    do {
        doSmth()
    } while (true)
}

fun t3() {
    return

    try {
        doSmth()
    }
    finally {
        doSmth()
    }
}

fun t4() {
    return

    (43)
}

fun doSmth() {}

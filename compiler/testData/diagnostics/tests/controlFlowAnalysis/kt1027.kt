//KT-1027 Strange selection of unreachable code

package kt1027

fun foo(<!UNUSED_PARAMETER!>c<!>: List<Int>) {
    var <!UNUSED_VARIABLE!>i<!> = 2

    return

    <!UNREACHABLE_CODE!>for (j in c) {  //strange selection of unreachable code
        i += 23
    }<!>
}

fun t1() {
    return

    <!UNREACHABLE_CODE!>while(true) {
        doSmth()
    }<!>
}

fun t2() {
    return

    <!UNREACHABLE_CODE!>do {
        doSmth()
    } while (true)<!>
}

fun t3() {
    return

    <!UNREACHABLE_CODE!>try {
        doSmth()
    }
    finally {
        doSmth()
    }<!>
}

fun t4() {
    return

    <!UNREACHABLE_CODE!>(43)<!>
}

fun doSmth() {}

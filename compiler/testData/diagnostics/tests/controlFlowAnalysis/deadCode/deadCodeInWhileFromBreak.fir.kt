fun foo(a: Any) {}
fun bar(a: Any, b: Any) {}

fun test(arr: Array<Int>) {
    while (true) {
        foo(break)
    }


    while (true) {
        bar(arr, break)
    }

    while (true) {
        arr[break]
    }

    while (true) {
        arr[1] = break
    }

    while (true) {
        break
        foo(1)
    }

    while (true) {
        var x = 1
        break
        x = 2
    }

    while (true) {
        var x = 1
        x = break
    }

    // TODO: bug, should be fixed in CFA
    while (true) {
        if (1 > 2 && break && 2 > 3) {

        }
    }

    // TODO: bug, should be fixed in CFA
    while (true) {
        if (1 > 2 || break || 2 > 3) {

        }
    }

    while (true) {
        break <!USELESS_ELVIS!>?: null<!>
    }
}

// !WITH_NEW_INFERENCE
package b

fun bar(i: Int) = i

fun test(a: Int?, b: Int?) {
    bar(<!NI;TYPE_MISMATCH!>if (a == null) return else <!OI;TYPE_MISMATCH!>b<!><!>)
}

fun test(a: Int?, b: Int?, c: Int?) {
    bar(<!NI;TYPE_MISMATCH!>if (a == null) return else if (b == null) return else <!OI;TYPE_MISMATCH!>c<!><!>)
}

fun test(a: Any?, b: Any?, c: Int?) {
    bar(<!NI;TYPE_MISMATCH!>if (a == null) if (b == null) <!OI;TYPE_MISMATCH!>c<!> else return else return<!>)
}

fun test(a: Int?, b: Any?, c: Int?) {
    bar(<!NI;TYPE_MISMATCH!>if (a == null) {
        return
    } else {
        if (b == null) {
            return
        } else {
            <!OI;TYPE_MISMATCH!>c<!>
        }
    }<!>)
}

// !WITH_NEW_INFERENCE
package b

fun bar(i: Int) = i

fun test(a: Int?, b: Int?) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (a == null) return else b)
}

fun test(a: Int?, b: Int?, c: Int?) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (a == null) return else if (b == null) return else c)
}

fun test(a: Any?, b: Any?, c: Int?) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (a == null) if (b == null) c else return else return)
}

fun test(a: Int?, b: Any?, c: Int?) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (a == null) {
        return
    } else {
        if (b == null) {
            return
        } else {
            c
        }
    })
}

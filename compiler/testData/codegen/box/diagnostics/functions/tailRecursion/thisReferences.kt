// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class A {
    tailrec fun f1(c : Int) {
        if (c > 0) {
            this.f1(c - 1)
        }
    }

    tailrec fun f2(c : Int) {
        if (c > 0) {
            f2(c - 1)
        }
    }

    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun f3(a : A) {
        a.<!NON_TAIL_RECURSIVE_CALL!>f3<!>(a) // non-tail recursion, could be potentially resolved by condition if (a == this) f3() else a.f3()
    }
}

fun box() : String {
    A().f1(1000000)
    A().f2(1000000)
    return "OK"
}

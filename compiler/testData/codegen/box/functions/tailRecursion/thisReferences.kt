class A {
    tailRecursive fun f1(c : Int, x : Any) {
        if (c > 0) {
            this.f1(c - 1, "tail")
        }
    }

    tailRecursive fun f2(c : Int, x : Any) {
        if (c > 0) {
            f2(c - 1, "tail 108")
        }
    }

    tailRecursive fun f3(a : A, x : Any) {
        a.f3(a, "no tail") // non-tail recursion, could be potentially resolved by condition if (a == this) f3() else a.f3()
    }
}

fun box() : String {
    A().f1(1000000, "test")
    A().f2(1000000, "test")
    return "OK"
}
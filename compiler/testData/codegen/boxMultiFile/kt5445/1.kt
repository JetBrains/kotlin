package test2

import test.A

public fun box(): String {
    return B().test(B())
}

public class B : A() {
    public fun test(other:Any): String {
        if (other is B && other.s == 2) {
            return "OK"
        }
        return "fail"
    }
}
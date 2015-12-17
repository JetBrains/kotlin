package test2

import test.A

class C : A() {
    fun a(): String {
        return this.s
    }
}

public fun box(): String {
    return C().a()
}

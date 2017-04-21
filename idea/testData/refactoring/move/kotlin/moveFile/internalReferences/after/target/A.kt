package target

import library.B
import library.bar

public class A: B() {
    fun test() {
        bar()
        foo()
        this.bar()
        this.foo()
    }
}

public fun test(a: A) {
    a.bar()
    a.foo()
    B().foo().bar()
    library.B().foo().bar()
    B().bar().foo()
}
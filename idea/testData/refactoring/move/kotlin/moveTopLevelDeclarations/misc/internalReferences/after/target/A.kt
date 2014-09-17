package target

import library.bar
import library.B

public class A: B() {
    fun test() {
        bar()
        foo()
        this.bar()
        this.foo()
        B().foo().bar()
        B().foo().bar()
        B().bar().foo()
    }
}
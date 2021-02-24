package b

import a.A

class B (val a: A) {
    fun baz() {
        a.foo()
    }
}
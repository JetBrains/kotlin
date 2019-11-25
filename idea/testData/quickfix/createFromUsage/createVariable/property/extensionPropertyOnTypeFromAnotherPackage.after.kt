// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

import package1.A

private val package2.A.foo: Any
    get() {
        TODO("Not yet implemented")
    }

class X {
    init {
        val y = package2.A()
        val foo = y.foo
    }
}
// "Create extension function 'foo'" "true"
// ERROR: Unresolved reference: foo

import package1.A

class X {
    init {
        val y = package2.A()
        y.foo()
    }
}

fun package2.A.foo() {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}

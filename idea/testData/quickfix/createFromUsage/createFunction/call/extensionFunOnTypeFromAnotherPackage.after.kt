// "Create extension function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

import package1.A

class X {
    init {
        val y = package2.A()
        y.foo()
    }
}

private fun package2.A.foo() {
    TODO("Not yet implemented")
}

// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

import package1.A

class X {
    init {
        val y = package2.A()
        val foo = y.<caret>foo
    }
}
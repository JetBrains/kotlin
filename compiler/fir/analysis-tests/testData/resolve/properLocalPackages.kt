package a.b

class X {
    fun foo() {
        class U {
            inner class K {
                inner class D {
                    fun check() {
                        class F {
                            inner class L
                        }
                    }
                }
            }

            <!NESTED_CLASS_NOT_ALLOWED!>class T<!>
        }
    }
}

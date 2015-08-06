// http://youtrack.jetbrains.net/issue/KT-449

class A {
    inner class B {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object { }
    }
}

class B {
    companion object {
        class B {
            companion object {
                class C {
                    companion object { }
                }
            }
        }
    }
}

class C {
    class D {
        companion object { }
    }
}
// http://youtrack.jetbrains.net/issue/KT-449

class A {
    inner class B {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>class object<!> { }
    }
}

class B {
    class object {
        class B {
            class object {
                class C {
                    class object { }
                }
            }
        }
    }
}

class C {
    class D {
        class object { }
    }
}
// http://youtrack.jetbrains.net/issue/KT-449

class A {
    inner class B {
        default <!DEFAULT_OBJECT_NOT_ALLOWED!>object<!> { }
    }
}

class B {
    default object {
        class B {
            default object {
                class C {
                    default object { }
                }
            }
        }
    }
}

class C {
    class D {
        default object { }
    }
}
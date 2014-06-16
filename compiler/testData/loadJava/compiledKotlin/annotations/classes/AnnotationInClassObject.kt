package test

class A {
    class object {
        annotation class Anno1

        class B {
            annotation class Anno2
        }
    }
}

A.Anno1 A.B.Anno2 class C
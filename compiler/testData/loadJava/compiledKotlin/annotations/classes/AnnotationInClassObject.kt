package test

class A {
    default object {
        annotation class Anno1

        class B {
            annotation class Anno2
        }
    }
}

A.Default.Anno1 A.Default.B.Anno2 class C
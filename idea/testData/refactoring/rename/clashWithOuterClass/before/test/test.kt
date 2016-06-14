package test

class A {
    inner class /*rename*/B {
        inner class C {
            val b: B = B()
        }
    }
}
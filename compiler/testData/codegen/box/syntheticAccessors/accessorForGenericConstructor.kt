fun box(): String {
    A.Nested().nestedA()
    A.Nested().Inner().innerA()
    A.companionA()
    return "OK"
}

class A<T> private constructor(val x: T, val y: Int = 0) {
    class Nested {
        fun nestedA() = A<Long>(1L)

        inner class Inner {
            fun innerA() = A<Long>(1L)
        }
    }

    companion object {
        fun companionA() = A<Long>(1L)
    }
}
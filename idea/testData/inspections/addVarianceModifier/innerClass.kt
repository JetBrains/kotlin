
abstract class TestOut<T> {
    abstract fun test(): T

    abstract inner class InnerTest {
        abstract fun innerTest(test: T)
    }
}

abstract class TestIn<T> {
    abstract fun test(test: T)

    abstract inner class InnerTest {
        abstract fun innerTest(): T
    }
}
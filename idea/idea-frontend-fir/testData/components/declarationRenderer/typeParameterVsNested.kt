package test

interface Some

abstract class My<T : Some> {
    open inner class T

    abstract val x: T

    abstract fun foo(arg: T)

    abstract val y: My<test.Some>.T

    abstract val z: test.My<test.Some>.T

    abstract class Some : My<test.Some>.T()
}
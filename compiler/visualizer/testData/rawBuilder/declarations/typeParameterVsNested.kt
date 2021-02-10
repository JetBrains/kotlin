// FIR_IGNORE
package test

interface Some

abstract class My<T : Some> {
    inner class T

//               T
//               │
    abstract val x: T

    abstract fun foo(arg: T)

//               [ERROR : T]
//               │  class My<T : Some>
//               │  │
    abstract val y: My.T

//               [ERROR : T]
//               │       class My<T : Some>
//               │       │
    abstract val z: test.My.T

//               [ERROR : T]
//               │
    class Some : T()
}

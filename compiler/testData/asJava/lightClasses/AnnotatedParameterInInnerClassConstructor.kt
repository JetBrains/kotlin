// test.AnnotatedParameterInInnerClassConstructor
package test

annotation class Anno(val x: String)

class AnnotatedParameterInInnerClassConstructor {

    inner class Inner(@Anno("a") a: String, @Anno("b") b: String) {

    }

    inner class InnerGeneric<T>(@Anno("a") a: T, @Anno("b") b: String) {

    }
}
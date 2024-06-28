// FIR_IDENTICAL
package test

annotation class AnnoA
annotation class AnnoB

class AnnotatedParameterInInnerClassConstructor {

    inner class Inner(@AnnoA a: String, @AnnoB b: String) {

    }

    inner class InnerGeneric<T>(@AnnoA a: T, @AnnoB
    b: String) {

    }
}
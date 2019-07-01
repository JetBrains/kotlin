// IGNORE_BACKEND: JVM_IR
package test

class Outer<T> {
    inner class Inner(list: List<T>)

    inner class InnerSimple()
}
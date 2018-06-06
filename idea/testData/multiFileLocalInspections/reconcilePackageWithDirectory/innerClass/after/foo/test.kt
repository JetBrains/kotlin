package foo

open class Class1() {
    init { InnerClass() }
    open inner class InnerClass() { }
}
package b

var result = "fail"

abstract class A {
    protected fun foo() {
        result = "OK"
    }
}

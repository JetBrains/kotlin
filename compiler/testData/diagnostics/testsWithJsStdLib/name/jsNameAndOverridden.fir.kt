package foo

open class Super {
    fun foo() = 23
}

class Sub : Super() {
    @JsName("foo") fun bar() = 42
}

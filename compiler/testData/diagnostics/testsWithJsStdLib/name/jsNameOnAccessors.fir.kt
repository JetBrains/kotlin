package foo

class A {
    var x: Int
        @JsName("xx") get() = 0
        @JsName("xx") set(value) {}
}

// FIR_IDENTICAL
package foo

class A(val x: String) {
    @JsName("aa") <!JS_NAME_CLASH!>constructor(x: Int)<!> : this("int $x")
}

<!JS_NAME_CLASH!>fun aa()<!> {}

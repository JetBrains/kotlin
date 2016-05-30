package foo

class A(val x: String) {
    <!JS_NAME_CLASH!>@JsName("aa") constructor(x: Int) : this("int $x")<!>
}

<!JS_NAME_CLASH!>fun aa() {}<!>
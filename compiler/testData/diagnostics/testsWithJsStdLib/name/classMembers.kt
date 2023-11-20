// FIR_IDENTICAL
// !DIAGNOSTICS: -ERROR_SUPPRESSION
@file:Suppress("UNUSED_PARAMETER", "CONFLICTING_OVERLOADS", "REDECLARATION")

class A1 {
    fun foo(x: Int) {}
    fun foo(x: String) {}
}

class A2 {
    fun Int.foo() {}
    fun String.foo() {}
}

class A3 {
    fun foo(x: Int) {}
    fun Int.foo() {}
}

class A4 {
    fun foo(x: Int) {}
    val foo = 1
}

class A5 {
    <!JS_NAME_CLASH!>fun foo()<!> {}
    <!JS_NAME_CLASH!>val foo<!> = 1
}

class A6 {
    fun Int.foo() {}
    val foo = 1
}

class A7 {
    fun Int.foo() {}
    val Int.foo get() = 1
}

class A8 {
    val foo = 1
    val Int.foo get() = 1
}

class A9 {
    val String.foo get() = 1
    val Int.foo get() = 1
}

class A10 {
    fun foo(vararg x: Int) {}
    fun foo() {}
}

class A14 {
    fun foo(vararg x: Int) {}
    fun foo(x: Int) {}
}

class A15 {
    fun foo(vararg x: Int) {}
    fun foo(vararg x: String) {}
}

class A16 {
    fun foo(vararg x: Int) {}
    val foo = 1
}

class A17 {
    fun foo(vararg x: Int) {}
    val Int.foo get() = 1
}

class A18 {
    fun foo(vararg x: Int) {}
    fun Int.foo() = 1
}

class A19 {
    <!JS_NAME_CLASH!>fun setFoo()<!> {}
    var foo: Int
        @JsName("getFoo") get() = 1
        <!JS_NAME_CLASH!>@JsName("setFoo") set(value: Int)<!> {}
}

class A20 {
    fun setFoo(x: Int) {}
    var foo: Int
        @JsName("getFoo") get() = 1
        @JsName("setFoo") set(value: Int) {}
}

class A21 {
    <!JS_NAME_CLASH!>fun foo()<!> {}
    <!JS_NAME_CLASH!>@JsName("foo") fun bar()<!> {}
}

class A22 {
    fun foo(x: Int) {}
    @JsName("foo") fun bar() {}
}

class A23 {
    <!JS_NAME_CLASH!>val foo<!> = 1
    <!JS_NAME_CLASH!>@JsName("foo") fun bar()<!> {}
}

class A24 {
    <!JS_NAME_CLASH!>@JsName("foo") val bar<!> = 1
    <!JS_NAME_CLASH!>fun foo()<!> {}
}

class A25 {
    @JsName("foo") val bar = 1
    fun foo(x: Int) {}
}

class A26 {
    <!JS_NAME_CLASH!>fun foo()<!> {}
    <!JS_NAME_CLASH!>var foo: Int<!>
        get() = 1
        set(value: Int) {}
}

class A27 {
    <!JS_NAME_CLASH!>@JsName("foo") fun bar()<!> {}
    <!JS_NAME_CLASH!>var foo: Int<!>
        get() = 1
        set(value: Int) {}
}

class A28 {
    fun foo(x: Int) {}
    var foo: Int
        get() = 1
        set(value: Int) {}
}

class A29 {
    <!JS_NAME_CLASH!>val foo<!> get() = 1
    <!JS_NAME_CLASH!>@JsName("foo") fun bar()<!> {}
}

class A30 {
    val Int.foo get() = 1
    @JsName("foo") fun bar() {}
}

class A31 {
    <!JS_NAME_CLASH!>object foo<!>
    <!JS_NAME_CLASH!>fun foo()<!> {}
}

class A32 {
    object foo
    fun foo(x: Int) {}
}

class A33 {
    <!JS_NAME_CLASH!>object foo<!>
    <!JS_NAME_CLASH!>val foo<!> = 1
}

class A34 {
    object foo
    val String.foo get() = 1
}

class A35 {
    companion <!JS_NAME_CLASH!>object foo<!>
    <!JS_NAME_CLASH!>fun foo()<!> {}
}

class A36 {
    companion object foo
    fun foo(x: Int) {}
}

class A37 {
    companion <!JS_NAME_CLASH!>object foo<!>
    <!JS_NAME_CLASH!>val foo<!> = 1
}

class A38 {
    companion object foo
    val String.foo get() = 1
}

class A39 {
    class <!JS_NAME_CLASH!>foo<!>
    <!JS_NAME_CLASH!>fun foo()<!> {}
}

class A40 {
    class foo
    fun foo(x: Int) {}
}

class A41 {
    class <!JS_NAME_CLASH!>foo<!>
    <!JS_NAME_CLASH!>val foo<!> = 1
}

class A42 {
    class foo
    val String.foo get() = 1
}

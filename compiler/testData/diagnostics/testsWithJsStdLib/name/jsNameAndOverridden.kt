// FIR_IDENTICAL
package foo

open class Super {
    <!JS_NAME_CLASH!>fun foo()<!> = 23
}

class Sub : Super() {
    <!JS_NAME_CLASH!>@JsName("foo") fun bar()<!> = 42
}
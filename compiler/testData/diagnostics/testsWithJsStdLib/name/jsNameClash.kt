// FIR_IDENTICAL
package foo

<!JS_NAME_CLASH!>@JsName("x") fun foo(x: Int)<!> = x

<!JS_NAME_CLASH!>@JsName("x") fun bar()<!> = 42

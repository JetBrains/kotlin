// FIR_IDENTICAL
package foo

<!JS_NAME_CLASH!>@JsName("bar") private fun foo(x: Int)<!> = x

<!JS_NAME_CLASH!>fun bar()<!> = 42

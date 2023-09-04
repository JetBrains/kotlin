// FIR_IDENTICAL
package foo

class A

<!JS_NAME_CLASH!>@JsName("get_bar") fun A.get_bar()<!> = 23

val A.bar: Int
  <!JS_NAME_CLASH!>@JsName("get_bar") get()<!> = 42

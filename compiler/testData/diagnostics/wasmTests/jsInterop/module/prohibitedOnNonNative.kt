package foo

@JsModule("A")
class <!JS_MODULE_PROHIBITED_ON_NON_NATIVE!>A<!>

@JsModule("B")
<!JS_MODULE_PROHIBITED_ON_NON_NATIVE!>object B<!>

<!JS_MODULE_PROHIBITED_ON_NON_NATIVE!>@JsModule("foo")
fun foo()<!> = 23

<!JS_MODULE_PROHIBITED_ON_NON_NATIVE!>@JsModule("bar")
val bar<!> = 42

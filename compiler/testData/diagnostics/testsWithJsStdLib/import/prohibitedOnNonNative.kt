// FIR_IDENTICAL
package foo

@JsImport("A")
class <!JS_IMPORT_PROHIBITED_ON_NON_NATIVE!>A<!>

@JsImport("B")
<!JS_IMPORT_PROHIBITED_ON_NON_NATIVE!>object B<!>

<!JS_IMPORT_PROHIBITED_ON_NON_NATIVE!>@JsImport("foo")
fun foo()<!> = 23

<!JS_IMPORT_PROHIBITED_ON_NON_NATIVE!>@JsImport("bar")
val bar<!> = 42

<!JS_IMPORT_PROHIBITED_ON_NON_NATIVE!>@JsImport("baz")
val baz<!> = 99

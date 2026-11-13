// LANGUAGE: +RichErrors
// DUMP_IR
error class Foo
error class Bar

typealias R = String | Foo
typealias Errs = Foo | Bar

fun produce(fail: Boolean): R = if (fail) Foo() else "OK"
fun accept(x: R?): String = if (x is String) x else "FAIL"
fun flatten(x: R | Bar): String | Errs = x
fun errs(): Errs = Bar()

fun box(): String {
    if (produce(true) !is Foo) return "FAIL 1"
    if (flatten(Bar()) !is Bar) return "FAIL 2"
    if (errs() !is Bar) return "FAIL 3"
    val r: String | Foo = produce(false)
    if (flatten(r) !is String) return "FAIL 4"
    return accept(produce(false))
}

// IGNORE_BACKEND: JS_IR
fun StringBuilder.first() = this.get(0)

fun foo() = StringBuilder("foo").first()

fun box() = if (foo() == 'f') "OK" else "Fail ${foo()}"

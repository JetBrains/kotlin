// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR
// ^KT-86452
context(_: String) fun foo() {}
context(_: String, b: Boolean) fun foo2() {}

class Bar {
    context(_: String) fun baz() {}
    context(_: String, b: Boolean) fun baz2() {}
}

context(_: String) fun Int.qux() {}
context(_: String, b: Boolean) fun Int.qux2() {}

fun String.test() {
    ::foo
    Bar::baz
    Bar()::baz
    Int::qux
    1::qux
}

context(s: String, bb: Boolean)
fun test2() {
    ::foo2
    Bar::baz2
    Bar()::baz2
    Int::qux2
    1::qux2
}

context(_: String) suspend fun fooSuspend() {
    ::fooSuspend
}

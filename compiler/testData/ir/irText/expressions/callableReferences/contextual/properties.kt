// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR
// ^KT-86452
context(_: String) val foo get() = 1
context(_: String, b: Boolean) var foo2
    get() = 1
    set(value) {}

class Bar {
    context(_: String) val baz get() = 1
    context(_: String, b: Boolean) var baz2
        get() = 1
        set(value) {}
}

context(_: String) val Int.qux get() = 1
context(_: String, b: Boolean) var Int.qux2
    get() = 1
    set(value) {}

fun String.test() {
    ::foo
    val asFunctionType: () -> Int = ::foo
    Bar::baz
    Bar()::baz
    Int::qux
    1::qux
}

context(s: String, bb: Boolean)
fun test2(){
    ::foo2
    Bar::baz2
    Bar()::baz2
    Int::qux2
    1::qux2
}

interface A
interface Foo {
    operator fun A.invoke()
}

fun test(a: A, foo: Foo) {
    a.<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>foo<!>()
}

fun test(a: Int, foo: Int.()->Unit) {
    a.foo()
}
// FIR_IDENTICAL
// DONT_TARGET_EXACT_BACKEND: JS_IR

fun interface Foo : () -> Int

fun id(foo: Foo): Any = foo

fun test(foo: Foo) {
    id(foo)
}

// FIR_IDENTICAL
// DONT_TARGET_EXACT_BACKEND: JS_IR

// KT-42199
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

fun interface Foo : () -> Int

fun id(foo: Foo): Any = foo

fun test(foo: Foo) {
    id(foo)
}

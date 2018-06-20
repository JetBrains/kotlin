// !LANGUAGE: +NestedClassesInAnnotations
// IGNORE_BACKEND: JS_IR

annotation class Foo(val kind: Kind) {
    enum class Kind { FAIL, OK }
}

@Foo(Foo.Kind.OK)
fun box(): String {
    return Foo.Kind.OK.name
}

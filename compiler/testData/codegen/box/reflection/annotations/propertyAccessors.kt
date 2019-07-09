// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

annotation class Get
annotation class Set
annotation class SetParam

var foo: String
    @Get get() = ""
    @Set set(@SetParam value) {}

fun box(): String {
    assert(::foo.getter.annotations.single() is Get)
    assert(::foo.setter.annotations.single() is Set)
    assert(::foo.setter.parameters.single().annotations.single() is SetParam)

    return "OK"
}

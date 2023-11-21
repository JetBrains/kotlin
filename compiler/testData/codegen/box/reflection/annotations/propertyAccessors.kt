// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

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

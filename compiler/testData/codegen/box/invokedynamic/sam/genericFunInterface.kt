// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFoo<T> {
    fun foo(x: T): T
}

fun foo(fs: IFoo<String>) = fs.foo("O")

fun box() = foo { "${it}K" }
// MODULE: lib
// WITH_RUNTIME
// FILE: lib.kt

@JvmInline
value class Z private constructor(private val value: Any?) {
    fun result(): String = value as String

    companion object {
        fun create(value: Any?): Z = Z(value)
    }
}

fun interface IFoo<T> {
    fun foo(x: T): String
}

fun foo1(fs: IFoo<Z>) = fs.foo(Z.create("OK"))

// MODULE: main(lib)
// FILE: main.kt

fun box(): String =
    foo1 { it.result() }

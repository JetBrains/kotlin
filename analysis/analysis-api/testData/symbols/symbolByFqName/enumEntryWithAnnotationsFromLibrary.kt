// callable: lib/MyEnum.Foo
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

@RequiresOptIn
annotation class MyOptIn

annotation class Anno(val value: String)

enum class MyEnum {
    @MyOptIn
    @Anno("foo")
    Foo,

    Bar,
}

// MODULE: main(lib)
// FILE: main.kt
fun main() {
}

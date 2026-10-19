// callable: lib/MyEnum.Foo
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

enum class MyEnum {
    @Deprecated("Use Bar")
    Foo,

    Bar,
}

// MODULE: main(lib)
// FILE: main.kt
fun main() {
}

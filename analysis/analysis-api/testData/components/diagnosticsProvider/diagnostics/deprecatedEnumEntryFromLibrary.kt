// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

enum class MyEnum {
    @Deprecated("Use Baz")
    Foo,

    @Deprecated("Use Baz", level = DeprecationLevel.ERROR)
    Bar,

    Baz,
}

@Deprecated("Use MyEnum.Baz")
fun deprecatedFunction() {}

// MODULE: main(lib)
// FILE: main.kt
import lib.*

enum class SourceEnum {
    @Deprecated("Use Baz")
    Foo,

    Baz,
}

fun test() {
    MyEnum.Foo.toString()
    MyEnum.Bar.toString()
    MyEnum.Baz.toString()
    deprecatedFunction()
    SourceEnum.Foo.toString()
}

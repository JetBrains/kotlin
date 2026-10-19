// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class MyOptIn

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class MyErrorOptIn

enum class MyEnum {
    @MyOptIn
    Foo,

    @MyErrorOptIn
    Bar,

    Baz,
}

// MODULE: main(lib)
// FILE: main.kt
import lib.MyEnum
import lib.MyErrorOptIn
import lib.MyOptIn

fun noOptIn() {
    MyEnum.Foo.toString()
    MyEnum.Bar.toString()
    MyEnum.Baz.toString()
}

@OptIn(MyOptIn::class, MyErrorOptIn::class)
fun withOptIn() {
    MyEnum.Foo.toString()
    MyEnum.Bar.toString()
}

// WITH_STDLIB
// MODULE: lib-common
// FILE: lib.kt
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

// MODULE: app-common(lib-common)
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
    <!DEPRECATION!>deprecatedFunction<!>()
    SourceEnum.<!DEPRECATION!>Foo<!>.toString()
}

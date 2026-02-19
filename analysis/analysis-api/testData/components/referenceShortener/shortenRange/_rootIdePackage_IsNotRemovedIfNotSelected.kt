// FILE: main.kt
package test

fun usage(
    foo: _root_ide_package_.dependency.Foo
) {
    <expr>"random expr"</expr>

    _root_ide_package_.dependency.Foo.bar()
}

// FILE: dependency.kt
package dependency

object Foo {
    fun bar() {}
}
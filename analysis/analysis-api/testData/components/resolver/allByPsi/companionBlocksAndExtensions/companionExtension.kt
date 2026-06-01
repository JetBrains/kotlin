// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C

companion fun C.foo() {}
companion val C.prop: Int get() = 1

fun usage() {
    C.foo()
    C.prop

    C::foo
    C::prop
}

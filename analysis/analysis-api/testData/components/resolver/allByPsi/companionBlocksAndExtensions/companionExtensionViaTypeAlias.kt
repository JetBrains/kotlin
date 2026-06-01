// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C

typealias TA = C

companion fun C.foo() {}
companion val C.prop: Int get() = 1

fun usage() {
    TA.foo()
    TA.prop

    TA::foo
}

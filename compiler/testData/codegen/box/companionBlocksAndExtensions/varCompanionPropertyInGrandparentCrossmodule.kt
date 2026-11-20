// LANGUAGE: +CompanionBlocks
// MODULE: lib
open class A { companion { var p = "A" } }
open class B : A()

// MODULE: main(lib)
class C : B()

fun box() = "OK"

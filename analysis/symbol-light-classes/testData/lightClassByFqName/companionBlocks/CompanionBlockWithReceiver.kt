// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// COMPILATION_ERRORS
package one

class Foo

class C {
    companion {
        fun Foo.greet(): String = "Hi"
        val Foo.title: String get() = "C"
    }
}

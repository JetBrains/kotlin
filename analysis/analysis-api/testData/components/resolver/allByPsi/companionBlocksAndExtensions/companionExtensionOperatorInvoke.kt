// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C

companion operator fun C.invoke() {}

fun usage() {
    C()
    C.invoke()
}

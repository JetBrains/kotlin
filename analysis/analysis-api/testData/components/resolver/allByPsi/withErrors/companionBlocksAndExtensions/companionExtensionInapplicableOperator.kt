// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C

companion operator fun C.plus(i: Int): C = C()

fun usage() {
    C.plus(1)
}

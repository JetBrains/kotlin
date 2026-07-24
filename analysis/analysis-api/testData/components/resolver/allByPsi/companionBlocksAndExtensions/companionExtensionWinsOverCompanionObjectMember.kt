// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    companion object {
        fun same() = 1
    }
}

companion fun C.same() = ""

fun usage() {
    C.same()
}

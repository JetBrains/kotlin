// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    constructor()
    constructor()
    constructor(s: String)
}

companion operator fun C.invoke() {}
companion operator fun C.invoke() {}
companion operator fun C.invoke(withParam: Int) {}

fun usage() {
    C()
    C.invoke()
}

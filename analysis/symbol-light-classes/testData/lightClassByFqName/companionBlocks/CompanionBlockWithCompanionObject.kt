// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
package one

class C {
    companion {
        fun blockFun(): Int = 1
        val blockVal: String = "block"
    }

    companion object {
        fun companionObjectFun(): Int = 2
        val companionObjectVal: String = "companion"
    }
}

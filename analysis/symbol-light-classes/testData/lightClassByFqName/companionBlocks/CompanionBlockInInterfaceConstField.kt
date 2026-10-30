// one.WithConst
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
package one

interface WithConst {
    companion {
        const val INT: Int = 1
        const val STRING: String = "s"
        val plain: Int = 2
    }
}

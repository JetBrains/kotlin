// one.WithConst
// LANGUAGE: +CompanionBlocks
package one

interface WithConst {
    companion {
        const val INT: Int = 1
        const val STRING: String = "s"
        val plain: Int = 2
    }
}

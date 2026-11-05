// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-91345

fun String.test(): Int {
    val o = object {
        companion {
            val Int.q: Int get() = <expr>this</expr>
        }
    }
    return 0
}

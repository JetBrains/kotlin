// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-91345

fun String.test(): Int {
    val o = object {
        companion {
            fun foo(): Int = <expr>length</expr>
        }
    }
    return 0
}

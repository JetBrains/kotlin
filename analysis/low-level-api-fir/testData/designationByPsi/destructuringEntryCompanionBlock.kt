// KT-90199
// LANGUAGE: +CompanionBlocks
class A {
    companion {
        val (aa<caret>a, b) = 0 to 1
    }
}

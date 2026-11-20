// KT-90199
// LANGUAGE: +CompanionBlocks
data class MyPair(val i: Int, val b: Int)

class A {
    companion {
        val (first, la<caret>st) = MyPair(1, 2)
    }
}

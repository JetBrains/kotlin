// LANGUAGE: +CompanionBlocks
data class X(val a: Int, val b: Int)

class A {
    companion {
        val (a, b) = X(1, 2)
        var [c, d] = X(1, 2)
    }
}

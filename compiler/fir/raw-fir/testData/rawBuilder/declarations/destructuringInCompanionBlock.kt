// LANGUAGE: +CompanionBlocks
annotation class Anno

data class X(val a: Int, val b: Int)

class A {
    companion {
        val (a, @Anno b) = X(1, 2)
        var [c, d: Int] = X(1, 2)
    }
}

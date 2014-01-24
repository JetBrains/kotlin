class C {
    fun get(f: (Int) -> Int, x: Int) = f(x)
    fun test() = C().g<caret>et({ it + 1 }, 41)
}

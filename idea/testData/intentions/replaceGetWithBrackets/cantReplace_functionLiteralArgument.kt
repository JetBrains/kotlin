// IS_APPLICABLE: false
class C {
    fun get(x: Int, f: (Int) -> Int) = f(x)
    fun test() = C().g<caret>et(41) { it + 1 }
}

// IS_APPLICABLE: false
class C {
    fun get<T>(x: Int) = 42
    fun test() = g<caret>et<Int>(42)
}

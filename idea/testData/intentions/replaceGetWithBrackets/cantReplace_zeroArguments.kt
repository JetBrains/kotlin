// IS_APPLICABLE: false
class C {
    fun get() = 41
    fun test() = C().g<caret>et() + 1
}

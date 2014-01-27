// IS_APPLICABLE: false
class C {
    fun get(index : Int) = 41
    fun test() = g<caret>et(0) + 1
}

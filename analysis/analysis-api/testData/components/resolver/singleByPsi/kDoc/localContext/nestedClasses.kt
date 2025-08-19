/**
 * [A<caret_1>A] - to the top-level class AA
 * [AA.A<caret_2>A] - to the nested class AA
 */
class AA {
    /** [A<caret_3>A] - to the nested class AA */
    class AA
    /** [A<caret_4>A] - to fun AA(p: Int) */
    fun AA(p: Int) {
    }
}
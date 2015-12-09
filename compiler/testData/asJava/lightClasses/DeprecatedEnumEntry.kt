// p.E
// test for KT-8874

package p

enum class E {
    @Deprecated("a")
    Entry1,
    Entry2,
    @Deprecated("b")
    Entry3
}
// p.E
// test for KT-8874
// WITH_STDLIB

package p

enum class E {
    @Deprecated("a")
    Entry1,
    Entry2,
    @Deprecated("b")
    Entry3
}

// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
// KT-3525
object B {
    class C
    class C

    val a : Int = 1
    val a : Int = 1
}
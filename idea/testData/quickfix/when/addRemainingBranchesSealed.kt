// "Add remaining branches" "true"
sealed class Variant {
    object Singleton : Variant()

    class Something(val x: Int) : Variant()

    object Another : Variant()
}
fun test(v: Variant?) = wh<caret>en(v) {
    Variant.Singleton -> "s"
}

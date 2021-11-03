// !DIAGNOSTICS: -UNCHECKED_CAST

context(Any)
fun f(g: context(Any) () -> Unit, value: Any): context(A) () -> Unit {
    return value as (context(A) () -> Unit)
}

context(String, Int)
class A {
    context(Any)
    val p: Any get() = 42

    context(String, Int)
    fun m() {}
}
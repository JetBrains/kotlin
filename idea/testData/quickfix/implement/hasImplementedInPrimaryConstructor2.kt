// "Implement members" "true"
// WITH_RUNTIME
abstract class B {
    abstract val p: String?
    abstract fun test()
}

<caret>class MyImpl2(
    override val p: String? = null
) : B() {
}
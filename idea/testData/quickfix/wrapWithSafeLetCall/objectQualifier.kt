// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

object Obj {
    fun foo(x: Int) = x
}
val arg: Int? = null
val argFoo = Obj.foo(<caret>arg)
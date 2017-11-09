// "Fix with 'asDynamic'" "true"
// JS

@native
class B {
    @na<caret>tiveSetter
    fun foo(i: Int, v: B)
}

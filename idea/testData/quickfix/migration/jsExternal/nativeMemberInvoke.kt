// "Fix with 'asDynamic'" "true"
// JS

@native
class B {
    @na<caret>tiveInvoke
    fun bar(a: B)
}

// "Fix with 'asDynamic'" "true"
// JS
class A

@native
class B<T: A> {
    @nat<caret>iveInvoke
    fun exp(t: T)
}

// "Fix with 'asDynamic'" "true"
// JS
class A

@native
class B<T: A> {
    @nat<caret>iveInvoke
    fun<T2> exp(t: T, t2: T2)
}

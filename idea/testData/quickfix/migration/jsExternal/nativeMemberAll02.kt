// "Fix with 'asDynamic'" "true"
// JS

@<caret>native
class B {
    @nativeGetter
    fun foo(i: Int): B?

    @na<caret>tiveSetter
    fun foo(i: Int, v: B)

    @nativeInvoke
    fun bar(a: B)

    @nativeInvoke
    fun<T> exp(t: T)

    fun dontTouch(): Nothing = definedExternally
}
// "Fix with 'asDynamic'" "true"
// JS
// ERROR: Declaration of such kind (extension function) cant be external

external class B {
    @nativeGetter
    fun foo(i: Int): B?

    @nati<caret>veSetter
    fun foo(i: Int, v: B)

    @nativeInvoke
    fun bar(a: B)

    @nativeInvoke
    fun<T> exp(t: T)

    fun dontTouch(): Nothing = definedExternally

    fun B.doNotTouchNestedExtensionMembers(): Nothing = definedExternally
}
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
interface A<T : A<T?>?> {
    fun foo(): T?
}
fun testA(a: A<*>) {
    // in new inference here we have A<A<A<*>>>
    a.foo() checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><A<*>?>() }
}

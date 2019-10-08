// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual open class <caret>A<T: A<T>> {
    actual class C<T>
    actual inner class B<F : C<C<C<A<T>>>>>
}
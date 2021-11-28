// FIR_IDENTICAL
//  Type inference failed after smart cast

interface A<T>
interface B<T> : A<T>

fun <T> foo(b: A<T>) = b

fun <T> test(a: A<T>) {
    if (<!USELESS_IS_CHECK!>a is Any<!>) {
        // Error:(9, 9) Kotlin: Type inference failed: fun <T> foo(b: A<T>): kotlin.Unit
        // cannot be applied to (A<T>)
        foo(a)
    }   
    foo(a) // ok
}

// "Create function 'invoke' from usage" "true"

class A<T>(val n: T)
class B<T>(val m: T)

fun test<U, V>(u: U): B<V> {
    return A(u)<caret>(u, "u")
}
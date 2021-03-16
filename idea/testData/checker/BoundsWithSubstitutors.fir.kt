// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

    open class A<T>
    class B<X : A<X>>()

    class C : A<C>()

    val a = B<C>()
    val a1 = B<Int>()

    class X<A, B : A>()

    val b = X<Any, X<A<C>, C>>()
    val b0 = X<Any, Any?>()
    val b1 = X<Any, X<A<C>, String>>()

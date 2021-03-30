// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

    open class A<T>
    class B<X : A<X>>()

    class C : A<C>()

    val a = B<C>()
    val a1 = B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A<kotlin/Int>'">Int</error>>()

    class X<A, B : A>()

    val b = X<Any, X<A<C>, C>>()
    val b0 = X<Any, <error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlin/Any'">Any?</error>>()
    val b1 = X<Any, <error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A<C>'">X<A<C>, String></error>>()

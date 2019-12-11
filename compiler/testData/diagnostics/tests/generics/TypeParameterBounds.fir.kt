// See KT-9438: Enforce the Single Instantiation Inheritance Rule for type parameters

interface A

interface B

interface D<T>

interface IncorrectF<T : D<A>> where T : D<B>

interface CorrectF<T> where T : D<A>, T : D<B>

interface G<T>

interface IncorrectH<T : G<D<A>>> where T : G<D<T>>

interface CorrectH<T> where T : G<D<A>>, T : G<D<B>>

interface incorrectJ<T: G<D<T>>> where T : G<D<T?>>

interface correctJ<T> where T : G<D<T>>, T : G<D<T?>>

fun <T> bar() where T : D<A>, T : D<B> {}

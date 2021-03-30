// See KT-9438: Enforce the Single Instantiation Inheritance Rule for type parameters

interface A

interface B

interface D<T>

interface CorrectF<T> where T : D<A>, T : D<B>

fun <T> bar() where T : D<A>, T : D<B> {}

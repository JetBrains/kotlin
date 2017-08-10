class Inv<K>
class Out<out K>
class In<in K>

fun <T> foo1(<warning>x</warning>: T, <warning>i1</warning>: Inv<T>, <warning>o1</warning>: Out<T>, <warning>y</warning>: T) {}

fun ttest(i: Inv<Int>, o: Out<String>) {
    foo1(1.0, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo1(x: T, i1: Inv<T>, o1: Out<T>, y: T): Unit
should be equal to: Int (for parameter 'i1')
should be a supertype of: Double (for parameter 'x'), String (for parameter 'o1')
">i</error>, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo1(x: T, i1: Inv<T>, o1: Out<T>, y: T): Unit
should be equal to: Int (for parameter 'i1')
should be a supertype of: Double (for parameter 'x'), String (for parameter 'o1')
">o</error>, "")
}

fun <T> foo2(<warning>inv</warning>: Inv<T>, <warning>o</warning>: Out<T>, <warning>i</warning>: In<T>) {}

class A
class B
class C
fun bar(o: Out<B>, i: Inv<C>, a: In<A>) {
    foo2(i, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo2(inv: Inv<T>, o: Out<T>, i: In<T>): Unit
should be a subtype of: A (for parameter 'i')
should be equal to: C (for parameter 'inv')
should be a supertype of: B (for parameter 'o')
">o</error>, <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> foo2(inv: Inv<T>, o: Out<T>, i: In<T>): Unit
should be a subtype of: A (for parameter 'i')
should be equal to: C (for parameter 'inv')
should be a supertype of: B (for parameter 'o')
">a</error>)
}

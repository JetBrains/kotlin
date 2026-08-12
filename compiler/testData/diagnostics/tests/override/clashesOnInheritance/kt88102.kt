// RUN_PIPELINE_TILL: FRONTEND
// SCOPE_DUMP: P:foo, Q:foo, GP:foo, GP2:foo

interface A {
    fun foo(): A
}

interface B {
    fun foo(): A
}

interface AB : A, B

interface C {
    fun foo(): C
}

object Impl : A, GA<A> {
    override fun foo(): A = this
}

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class P<!> : A by Impl, AB, C
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Q<!> : A by Impl, C
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class R<!> : A by Impl, B, C

interface GA<T> {
    fun foo(): T
}

interface GB<T> {
    fun foo(): T
}

interface GAB<T> : GA<T>, GB<T>
interface GAB2 : GA<A>, GB<A>

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class GP<!> : GA<A> by Impl, GAB<A>, C
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class GP2<!> : GAB2, GA<A> by Impl, C

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration,
objectDeclaration, override, thisExpression */

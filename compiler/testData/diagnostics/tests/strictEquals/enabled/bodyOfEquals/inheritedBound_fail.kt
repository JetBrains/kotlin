// RUN_PIPELINE_TILL: FRONTEND

class C : I {
    val c: Int get() = 42
    override fun equals(other: Any?): Boolean = c == other.<!UNRESOLVED_REFERENCE!>c<!>
}

abstract class B : I {
    val b: Int get() = 42
}

abstract class A : B() {
    abstract override fun equals(@EqualityBound(C::class) other: Any?): Boolean
}

class Z : A() {
    override fun equals(other: Any?): Boolean = b == other.<!UNRESOLVED_REFERENCE!>b<!>
}

class F : G<Int>() {
    override fun equals(other: Any?): Boolean = g == other.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>g<!>
}

interface I {
    val i: Int get() = 42
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

open class G<X> : I {
    override fun equals(@EqualityBound(G::class) other: Any?): Boolean = super.equals(other)
}

val G<Int>.g: Int get() = 42

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, nullableType, operator, override, propertyDeclaration, smartcast */

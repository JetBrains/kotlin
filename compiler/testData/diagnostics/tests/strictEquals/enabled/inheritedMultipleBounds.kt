// RUN_PIPELINE_TILL: FRONTEND

object Unrelated

interface Bounded {
    override fun equals(@EqualityBound(Bounded::class) other: Any?): Boolean
}

interface SubBoundedNewBound : Bounded {
    override fun equals(@EqualityBound(SubBoundedNewBound::class) other: Any?): Boolean
}

interface SubBoundedSameBound : Bounded

interface Unbounded

class A1 : Bounded, Unbounded {
    override fun equals(other: Any?): Boolean = true
}

class A2 : Unbounded, Bounded {
    override fun equals(other: Any?): Boolean = true
}

class A3 : Unbounded, Bounded {
    override fun equals(@EqualityBound(Bounded::class) other: Any?): Boolean = true
}

class A4 : Unbounded, Bounded {
    override fun equals(@EqualityBound(Bounded::class) other: Any?): Boolean = true
}

class A5 : Bounded, Unbounded {
    override fun equals(@EqualityBound(SubBoundedSameBound::class) other: Any?): Boolean = true
}

class A6 : Bounded, Unbounded {
    override fun equals(@EqualityBound(SubBoundedNewBound::class) other: Any?): Boolean = true
}

fun test(a1: A1, a2: A2, a3: A3) {
    if (a1 == Unrelated) {}
    if (a2 == Unrelated) {}
    if (a3 == Unrelated) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, objectDeclaration, operator, override */

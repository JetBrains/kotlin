// RUN_PIPELINE_TILL: FRONTEND

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

interface J : I {
    override fun equals(@EqualityBound(J::class) other: Any?): Boolean
}

abstract class K : J {
    override fun equals(other: Any?): Boolean = true
}

class H : I, K() {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true<!>
}

class H2 : K(), I {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true<!>
}

class L : K(), I
class L2 : I, K()

interface I2 {
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

class Impl: I, I2 {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(I2::class) other: Any?): Boolean = true<!>
}

class Impl2 : I2, I {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(I2::class) other: Any?): Boolean = true<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override */

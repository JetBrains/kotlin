// RUN_PIPELINE_TILL: BACKEND

class C : I {
    override fun equals(other: Any?): Boolean {
        return i == other.i
    }
}

open class Generic<X> : B() {
    val g: Int get() = 42
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean {
        return b == other.b
    }
}

open class Generic2<X> : Generic<X>() {
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean {
        return super.equals(other)
    }
}

fun local() {
    val obj = object : Generic2<String>() {
        override fun equals(other: Any?) = other.g == g
    }

    class Local {
        val l: Int get() = 42
        override fun equals(@EqualityBound(Local::class) other: Any?) = other.l == l
    }

    open class LocalA<X> : Generic2<X>()
    open class LocalB<X> : LocalA<X>() {
        override fun equals(other: Any?) = true
    }
    open class LocalC<X> : LocalB<X>()
    class LocalD<X> : LocalC<X>() {
        override fun equals(other: Any?) = other.g == g
    }
}

abstract class B : I {
    val b: Int get() = 42
    override abstract fun equals(other: Any?): Boolean
}

class A : B() {
    override fun equals(other: Any?): Boolean {
        return i == other.i
    }
}

interface I {
    val i: Int get() = 42
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, equalityExpression,
functionDeclaration, getter, integerLiteral, interfaceDeclaration, localClass, localProperty, nullableType, operator,
override, propertyDeclaration, smartcast, starProjection, superExpression, typeParameter */

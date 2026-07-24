// RUN_PIPELINE_TILL: BACKEND

class Regular {
    fun useSite_1() {
        val x: Any? = Regular()
        if (this != x) null!!
        x.p1
    }
    val p1: String get() = "!"
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

fun useSite_2(x: Any?): Int? {
    if (Regular() == x) {
        return x.p1.length
    }
    return null
}

open class Super(var p2: String)

class Child : Super("!") {
    override fun equals(@EqualityBound(Super::class) other: Any?): Boolean = true
}

val Child.useSite_3 get() = run {
    var x: Any? = Super("!")
    if (this != x || x.p2.length == 0) return@run null
    x
}

abstract class Inheritable {
    override fun equals(@EqualityBound(Inheritable::class) other: Any?): Boolean = true
}

class Inherited : Inheritable()

fun useSite_4(x: Any?, y: Any?): Inheritable? {
    if (x !is Inherited) return null
    if (x != y) return null
    return y
}

class Generic<T : CharSequence>(val p3: T) {
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean = true

    fun useSite_5(other: Any): Boolean = this == other && other.p3.length == 42

    inner class Inner(val p4: T) {
        override fun equals(@EqualityBound(Inner::class) other: Any?): Boolean = true

        fun useSite_6(other: Any): Boolean = this == other && other.p4.length == 42
    }
}

/* GENERATED_FIR_TAGS: andExpression, checkNotNullCall, classDeclaration, classReference, disjunctionExpression,
equalityExpression, functionDeclaration, getter, ifExpression, integerLiteral, isExpression, lambdaLiteral,
localProperty, nullableType, operator, override, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
smartcast, stringLiteral, thisExpression, typeConstraint, typeParameter */

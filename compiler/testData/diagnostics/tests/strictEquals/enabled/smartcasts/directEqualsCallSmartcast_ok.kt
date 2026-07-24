// RUN_PIPELINE_TILL: BACKEND

// FILE: q/Utils.java
package q;

public class Utils {
    public static <T> T makeFlexible(T t) {
        return t;
    }
}

// FILE: q/main.kt
package q

fun <T> id(t: T): T = t

class Regular {
    fun r() = Unit
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

fun useSite_1(a: Regular, b: Regular?, c: Any?) {
    if (a.equals(c) || b?.equals(c) == true) {
        c.r()
    }
}

interface I1 {
    fun i1() = Unit
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2 {
    fun i2() = Unit
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

fun <T> useSite_2(t: T, c: Any?, d: Any?) where T : I1, T : I2 {
    if (t.equals(c)) {
        c.i1()
        c.i2()
        val e = Utils.makeFlexible(c)
        if (<!DEBUG_INFO_EXPRESSION_TYPE("(q.I1 & q.I2..q.I1? & q.I2?)")!>e<!>.equals(other = d)) {
            d.i1()
            <!DEBUG_INFO_EXPRESSION_TYPE("q.I1 & q.I2")!>d<!>.i2()
        }
    }
    if (d is I1? && d is I2? && d?.equals(c) == true) {
        c.i1()
        c.i2()
    }
    if (d is I1? && d is I2) {
        if (!d.equals(c)) return
        c.i1()
        c.i2()
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, disjunctionExpression, equalityExpression,
flexibleType, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType, isExpression, javaFunction,
localProperty, nullableType, operator, override, propertyDeclaration, safeCall, smartcast, typeConstraint, typeParameter */

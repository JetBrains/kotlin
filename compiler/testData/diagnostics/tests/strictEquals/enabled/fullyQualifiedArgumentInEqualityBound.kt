// RUN_PIPELINE_TILL: FRONTEND

// FILE: p1/p2/p3/A.kt
package p1.p2.p3

open class A

// FILE: q/B.kt

package q

class B : p1.p2.p3.A() {
    override fun equals(@EqualityBound(p1.p2.p3.A::class) other: Any?): Boolean {
        return super.equals(other)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, override, superExpression */

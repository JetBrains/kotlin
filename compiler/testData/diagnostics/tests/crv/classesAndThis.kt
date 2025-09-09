// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

class A(val x: String = "x") {
    fun foo(y: String): A {
        y // local, should not report
        x // unused, may have getter
        this // should not report
        A   // should not report
        Companion   // should not report
        return this // used
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as A

        if (x != <!DEBUG_INFO_SMARTCAST!>other<!>.x) return false

        return true
    }

    fun Int.foo() {
        this    // should not report
        this@A  // should not report
    }

    @MustUseReturnValues
    companion object
}

interface I {
    fun foo()
}

object Impl: I {
    override fun foo() {
        Impl
        TODO("Not yet implemented")
    }
}

class Impl2(): I by Impl

annotation class Bar(
    val a: IntArray = [1, 2],
    val b: IntArray = intArrayOf(1, 2)
)

fun main() {
    A()
    A().foo("x")
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, asExpression, classDeclaration,
classReference, collectionLiteral, companionObject, disjunctionExpression, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, inheritanceDelegation, integerLiteral, interfaceDeclaration, nullableType,
objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, thisExpression */

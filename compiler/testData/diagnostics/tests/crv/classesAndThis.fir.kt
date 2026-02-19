// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

class A(val x: String = "x") {
    fun foo(y: String): A {
        <!UNUSED_EXPRESSION!>y<!> // local, should not report
        <!RETURN_VALUE_NOT_USED!>x<!> // unused, may have getter
        <!UNUSED_EXPRESSION!>this<!> // should not report
        <!UNUSED_EXPRESSION!>A<!>   // should not report
        <!UNUSED_EXPRESSION!>Companion<!>   // should not report
        return this // used
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as A

        if (x != other.x) return false

        return true
    }

    fun Int.foo() {
        <!UNUSED_EXPRESSION!>this<!>    // should not report
        <!UNUSED_EXPRESSION!>this@A<!>  // should not report
    }

    @MustUseReturnValues
    companion object
}

interface I {
    fun foo()
}

object Impl: I {
    override fun foo() {
        <!UNUSED_EXPRESSION!>Impl<!>
        TODO("Not yet implemented")
    }
}

class Impl2(): I by Impl

annotation class Bar(
    val a: IntArray = [1, 2],
    val b: IntArray = intArrayOf(1, 2)
)

fun main() {
    <!RETURN_VALUE_NOT_USED!>A<!>()
    A().<!RETURN_VALUE_NOT_USED!>foo<!>("x")
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, asExpression, classDeclaration,
classReference, collectionLiteral, companionObject, disjunctionExpression, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, inheritanceDelegation, integerLiteral, interfaceDeclaration, nullableType,
objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, thisExpression */

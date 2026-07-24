// RUN_PIPELINE_TILL: FRONTEND

abstract class A

class B : A() {
    val prop: String = "!"
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean {
        return other.<!UNRESOLVED_REFERENCE!>prop<!>.length == prop.length
    }
}

open class Generic<T> {
    val t: T get() = null!!
}

class Impl : Generic<String>() {
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean {
        return other.t.<!UNRESOLVED_REFERENCE!>length<!> == 0
    }
}

fun <T> local() {
    fun T.doSomething(): Boolean = true

    class ImplicitGeneric {
        val t: T get() = null!!
        override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean {
            return other.t.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>doSomething<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, classReference, equalityExpression,
funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, localClass, localFunction, nullableType, operator,
override, propertyDeclaration, smartcast, starProjection, stringLiteral, typeParameter */
